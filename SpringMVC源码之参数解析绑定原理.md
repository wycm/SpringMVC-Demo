## 摘要
* 本文从源码层面简单讲解SpringMVC的参数绑定原理

## SpringMVC参数绑定相关组件的初始化过程
* 在理解初始化之前，先来认识一个接口

#### HandlerMethodArgumentResolver
* 方法参数解析器接口，这个接口是SpringMVC参数解析绑定的核心接口。不同的参数类型绑定都是通过实现这个接口来实现。也可以通过实现这个接口来自定义参数解析器。这个接口中有如下两个方法<br>

```
public interface HandlerMethodArgumentResolver {

    //该解析器是否支持parameter参数的解析
    boolean supportsParameter(MethodParameter parameter);

    //将方法参数从给定请求(webRequest)解析为参数值并返回
    Object resolveArgument(MethodParameter parameter,
                          ModelAndViewContainer mavContainer,
                          NativeWebRequest webRequest,
                          WebDataBinderFactory binderFactory) throws Exception;

}
```

#### 初始化
* RequestMappingHandlerAdapter.java类的afterPropertiesSet（line:481）方法初始化相关方法参数解析器。代码如下<br>

```
public void afterPropertiesSet() {
    if (this.argumentResolvers == null) {
        //初始化SpringMVC默认的方法参数解析器，并添加至argumentResolvers（HandlerMethodArgumentResolverComposite）
        List<HandlerMethodArgumentResolver> resolvers = getDefaultArgumentResolvers();
        this.argumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);
    }
    if (this.initBinderArgumentResolvers == null) {
        //初始化SpringMVC默认的初始化绑定器(@InitBinder)参数解析器，并添加至initBinderArgumentResolvers（HandlerMethodArgumentResolverComposite）
        List<HandlerMethodArgumentResolver> resolvers = getDefaultInitBinderArgumentResolvers();
        this.initBinderArgumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);
    }
    if (this.returnValueHandlers == null) {
        //获取默认的方法返回值解析器
        List<HandlerMethodReturnValueHandler> handlers = getDefaultReturnValueHandlers();
        this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite().addHandlers(handlers);
    }
    initControllerAdviceCache();
}
```
* 现在我们进入getDefalutArgumentResolvers方法，代码如下<br>

```
//默认的参数解析，创建了默认的24个参数解析器，并添加至resolvers
//这里的24个参数解析器都是针对不同的参数类型来解析的
private List<HandlerMethodArgumentResolver> getDefaultArgumentResolvers() {
    List<HandlerMethodArgumentResolver> resolvers = new ArrayList<HandlerMethodArgumentResolver>();

    // 基于注解的参数解析器

    //一般用于带有@RequestParam注解的简单参数绑定，简单参数比如byte、int、long、double、String以及对应的包装类型
    resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), false));
    //用于处理带有@RequestParam注解，且参数类型为Map的解析绑定
    resolvers.add(new RequestParamMapMethodArgumentResolver());
    //一般用于处理带有@PathVariable注解的默认参数绑定
    resolvers.add(new PathVariableMethodArgumentResolver());
    //也是用于带有@PathVariable注解的Map相关参数绑定，后续还有一些默认的参数解析器。后续还有一些参数解析器，我这里都不一一解释了。想具体确认某个参数会交个哪个参数解析器处理，可以通过以下解析器的supportsParameter(MethodParameter parameter)方法得知
    resolvers.add(new PathVariableMapMethodArgumentResolver());
    resolvers.add(new MatrixVariableMethodArgumentResolver());
    resolvers.add(new MatrixVariableMapMethodArgumentResolver());
    resolvers.add(new ServletModelAttributeMethodProcessor(false));
    resolvers.add(new RequestResponseBodyMethodProcessor(getMessageConverters()));
    resolvers.add(new RequestPartMethodArgumentResolver(getMessageConverters()));
    resolvers.add(new RequestHeaderMethodArgumentResolver(getBeanFactory()));
    resolvers.add(new RequestHeaderMapMethodArgumentResolver());
    resolvers.add(new ServletCookieValueMethodArgumentResolver(getBeanFactory()));
    resolvers.add(new ExpressionValueMethodArgumentResolver(getBeanFactory()));

    // 基于类型的参数解析器
    resolvers.add(new ServletRequestMethodArgumentResolver());
    resolvers.add(new ServletResponseMethodArgumentResolver());
    resolvers.add(new HttpEntityMethodProcessor(getMessageConverters()));
    resolvers.add(new RedirectAttributesMethodArgumentResolver());
    resolvers.add(new ModelMethodProcessor());
    resolvers.add(new MapMethodProcessor());
    resolvers.add(new ErrorsMethodArgumentResolver());
    resolvers.add(new SessionStatusMethodArgumentResolver());
    resolvers.add(new UriComponentsBuilderMethodArgumentResolver());

    // Custom arguments
    if (getCustomArgumentResolvers() != null) {
        resolvers.addAll(getCustomArgumentResolvers());
    }
```
* 参数解析器添加至HandlerMethodArgumentResolverComposite这个类，这个也是实现了HandlerMethodArgumentResolver接口。这里运用涉及模式中的composite模式（组合模式），SpringMVC中，所有请求的参数解析都是进入HandlerMethodArgumentResolverComposite类来完成的。它有两个成员变量，如下<br>

```
//它的元素在RequestMappingHandlerAdapter类的afterPropertiesSet方法中被添加，存放的是SpringMVC一些默认的HandlerMethodArgumentResolver参数解析器
private final List<HandlerMethodArgumentResolver> argumentResolvers =
            new LinkedList<HandlerMethodArgumentResolver>();
//存放已经解析过的参数，已经对应的HandlerMethodArgumentResolver解析器。加快查找过程
private final Map<MethodParameter, HandlerMethodArgumentResolver> argumentResolverCache =
        new ConcurrentHashMap<MethodParameter, HandlerMethodArgumentResolver>(256);
```
* 介绍了这么多，话不多说。直接来看一个详细解析绑定过程吧

## 绑定过程
* 先看一个简单参数绑定，有如下Controller和请求,代码如下。<br>

```
@Controller
@RequestMapping("/ParameterBind")
public class ParameterBindTestController {
    @ResponseBody
    @RequestMapping("/test1")
    public String test1(int id){
        System.out.println(id);
        return "test1";
    }
}
```
![](https://raw.githubusercontent.com/wycm/md-image/master/2018-02-08/1.png)
* 请求进入DispatcherServlet的doDispatch后，获取HandlerMethod。然后根据HandlerMethod来确认HandlerApapter，确认后执行HandlerAdapter的handle方法。这里确认HandlerApater为RequestMappingHandlerAdapter，在执行handlerMethod之前，需要处理参数的绑定。然后看看详细的参数绑定过程
* 执行HandlerAdapter的handler方法后，进入RequestMappingHandlerAdapter的invokeHandleMethod方法（line:711）<br>

```
private ModelAndView invokeHandleMethod(HttpServletRequest request,
            HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {

    ServletWebRequest webRequest = new ServletWebRequest(request, response);

    WebDataBinderFactory binderFactory = getDataBinderFactory(handlerMethod);
    ModelFactory modelFactory = getModelFactory(handlerMethod, binderFactory);
    //根据handlerMethod和binderFactory创建一个ServletInvocableHandlerMethod。后续把请求直接交给ServletInvocableHandlerMethod执行。
    //createRequestMappingMethod方法比较简单，把之前RequestMappingHandlerAdapter初始化的argumentResolvers和returnValueHandlers添加至ServletInvocableHandlerMethod中
    ServletInvocableHandlerMethod requestMappingMethod = createRequestMappingMethod(handlerMethod, binderFactory);

    ModelAndViewContainer mavContainer = new ModelAndViewContainer();
    mavContainer.addAllAttributes(RequestContextUtils.getInputFlashMap(request));
    modelFactory.initModel(webRequest, mavContainer, requestMappingMethod);
    mavContainer.setIgnoreDefaultModelOnRedirect(this.ignoreDefaultModelOnRedirect);

    AsyncWebRequest asyncWebRequest = WebAsyncUtils.createAsyncWebRequest(request, response);
    asyncWebRequest.setTimeout(this.asyncRequestTimeout);

    final WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
    asyncManager.setTaskExecutor(this.taskExecutor);
    asyncManager.setAsyncWebRequest(asyncWebRequest);
    asyncManager.registerCallableInterceptors(this.callableInterceptors);
    asyncManager.registerDeferredResultInterceptors(this.deferredResultInterceptors);

    if (asyncManager.hasConcurrentResult()) {
        Object result = asyncManager.getConcurrentResult();
        mavContainer = (ModelAndViewContainer) asyncManager.getConcurrentResultContext()[0];
        asyncManager.clearConcurrentResult();

        if (logger.isDebugEnabled()) {
            logger.debug("Found concurrent result value [" + result + "]");
        }
        requestMappingMethod = requestMappingMethod.wrapConcurrentResult(result);
    }

    requestMappingMethod.invokeAndHandle(webRequest, mavContainer);

    if (asyncManager.isConcurrentHandlingStarted()) {
        return null;
    }

    return getModelAndView(mavContainer, modelFactory, webRequest);
}
```
* 然后进入invokeAndHanldle方法，然后进入invokeForRequest方法，这个方法的职责是从request中解析出HandlerMethod方法所需要的参数，然后通过反射调用HandlerMethod中的method。代码如下<br>

```
public final Object invokeForRequest(NativeWebRequest request,
                                        ModelAndViewContainer mavContainer,
                                        Object... providedArgs) throws Exception {
        //从request中解析出HandlerMethod方法所需要的参数，并返回Object[]
        Object[] args = getMethodArgumentValues(request, mavContainer, providedArgs);

        if (logger.isTraceEnabled()) {
            StringBuilder builder = new StringBuilder("Invoking [");
            builder.append(this.getMethod().getName()).append("] method with arguments ");
            builder.append(Arrays.asList(args));
            logger.trace(builder.toString());
        }
        //通过反射执行HandleMethod中的method，方法参数为args。并返回方法执行的返回值
        Object returnValue = invoke(args);

        if (logger.isTraceEnabled()) {
            logger.trace("Method [" + this.getMethod().getName() + "] returned [" + returnValue + "]");
        }

        return returnValue;
    }
```
* 直接进入getMethodArgumentValues方法看看其过程，代码如下<br>

```
/**
* 获取当前请求的方法参数值。
*/
private Object[] getMethodArgumentValues(
        NativeWebRequest request, ModelAndViewContainer mavContainer,
        Object... providedArgs) throws Exception {
    //获取方法参数数组
    MethodParameter[] parameters = getMethodParameters();
    //创建一个参数数组，保存从request解析出的方法参数
    Object[] args = new Object[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
        MethodParameter parameter = parameters[i];
        parameter.initParameterNameDiscovery(parameterNameDiscoverer);
        GenericTypeResolver.resolveParameterType(parameter, getBean().getClass());

        args[i] = resolveProvidedArgument(parameter, providedArgs);
        if (args[i] != null) {
            continue;
        }
        //判断之前RequestMappingHandlerAdapter初始化的那24个HandlerMethodArgumentResolver（参数解析器），是否存在支持该参数解析的解析器
        if (argumentResolvers.supportsParameter(parameter)) {
            try {
                args[i] = argumentResolvers.resolveArgument(parameter, mavContainer, request, dataBinderFactory);
                continue;
            } catch (Exception ex) {
                if (logger.isTraceEnabled()) {
                    logger.trace(getArgumentResolutionErrorMessage("Error resolving argument", i), ex);
                }
                throw ex;
            }
        }

        if (args[i] == null) {
            String msg = getArgumentResolutionErrorMessage("No suitable resolver for argument", i);
            throw new IllegalStateException(msg);
        }
    }
    return args;
}
```
* 进入HandlerMethodArgumentResolverComposite的resolveArgument方法<br>

```
public Object resolveArgument(
            MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory)
            throws Exception {
        //首先获取参数解析器，这里获取的逻辑是首先从argumentResolverCache缓存中获取该MethodParameter匹配的HandlerMethodArgumentResolver。如果为空，遍历初始化定义的那24个。查找匹配的HandlerMethodArgumentResolver，然后添加至argumentResolverCache缓存中
        HandlerMethodArgumentResolver resolver = getArgumentResolver(parameter);
        Assert.notNull(resolver, "Unknown parameter type [" + parameter.getParameterType().getName() + "]");
        //解析参数
        return resolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
    }
```
* 然后进入HandlerMethodArgumentResolver的resolverArgument方法<br>

```
public final Object resolveArgument(
            MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory)
            throws Exception {
        //获取int的Class对象
        Class<?> paramType = parameter.getParameterType();
        //根据参数定义创建一个NamedValueInfo对象
        NamedValueInfo namedValueInfo = getNamedValueInfo(parameter);
        //根据参数名解析出对象的值
        Object arg = resolveName(namedValueInfo.name, parameter, webRequest);
        if (arg == null) {
            if (namedValueInfo.defaultValue != null) {
                arg = resolveDefaultValue(namedValueInfo.defaultValue);
            }
            else if (namedValueInfo.required) {
                handleMissingValue(namedValueInfo.name, parameter);
            }
            arg = handleNullValue(namedValueInfo.name, arg, paramType);
        }
        else if ("".equals(arg) && (namedValueInfo.defaultValue != null)) {
            arg = resolveDefaultValue(namedValueInfo.defaultValue);
        }
        //上面步骤获取的args是String类型，然后转换为方法参数所需要的类型(int)
        if (binderFactory != null) {
            WebDataBinder binder = binderFactory.createBinder(webRequest, null, namedValueInfo.name);
            arg = binder.convertIfNecessary(arg, paramType, parameter);
        }

        handleResolvedValue(arg, namedValueInfo.name, parameter, mavContainer, webRequest);

        return arg;
    }
```
* 这个方法的职责是，首先获取paramType。也就是int对应的Class对象。然后根据parameter对象创建一个NamedValueInfo对象。这个对象存放的就是参数名、是否必须、参数默认值3个成员变量。然后进入resolverName方法解析参数，里面的逻辑其实很简单，就是根据方法的参数名来获取request中的参数。关键代码如下<br>

```
String[] paramValues = webRequest.getParameterValues(name);
if (paramValues != null) {
    arg = paramValues.length == 1 ? paramValues[0] : paramValues;
}
```
所以这里返回的值就是9999,这里返回的值还是String类型的。而需要的参数是int类型的。然后通过binder.coverIfNecessary方法把String转换为int类型返回。

### 对象绑定
* 新加一个方法，代码如下
```
@ResponseBody
@RequestMapping("/test2")
public String test2(User u){
    System.out.println(u.toString());
    return "test1";
}
```
![](https://raw.githubusercontent.com/wycm/md-image/master/2018-02-11/1.jpg)

* 这个请求的参数解析绑定便会交给ServletModelAttributeMethodProcessor这个类，在初始化argumentResolvers的时候。是会创建两个不同的ServletModelAttributeMethodProcessor对象的。<br>

```
resolvers.add(new ServletModelAttributeMethodProcessor(false));
resolvers.add(new ServletModelAttributeMethodProcessor(true));
```
* 这两个有什么区别？进入supportsParameter方法看看<br>

```
/**
*带有@ModelAttribute注解返回true
* parameter不是简单类型也返回true.
*/
public boolean supportsParameter(MethodParameter parameter) {
        if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
            return true;
        }
        else if (this.annotationNotRequired) {
            return !BeanUtils.isSimpleProperty(parameter.getParameterType());
        }
        else {
            return false;
        }
    }
```
* 虽然test2(User u)方法没有ModelAttribute注解，但是User.class不是简单类型。所以该MethodHandler的参数u还是会交给ServletModelAtttributeMethodProcessor处理。
* 看看ServletModelAttributeMethodProcessor的resolveArgument方法。它的resolveArgument是由父类ModelAttributeMethodProcessor具体实现的，代码如下。<br>

```
/**
* 解析model中的参数，如果从ModelAndViewContainer未找到，直接通过反射实例化一个对象。具体实例化是通过父类的createAttribute方法，通过调用BeanUtils.instantiateClass方法来实例化的。这个对象便是后续传给test2(User u)方法的对象，但是此时创建的对象里面的值都还为空，注入值是通过bindRequestParameters方法来实现的。
*/
public final Object resolveArgument(
            MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest request, WebDataBinderFactory binderFactory)
            throws Exception {

        String name = ModelFactory.getNameForParameter(parameter);
        Object attribute = (mavContainer.containsAttribute(name)) ?
                mavContainer.getModel().get(name) : createAttribute(name, parameter, binderFactory, request);

        WebDataBinder binder = binderFactory.createBinder(request, attribute, name);
        if (binder.getTarget() != null) {
            //将请求绑定至目标binder的target对象，也就是刚刚创建的attribute对象。
            bindRequestParameters(binder, request);
            //如果有验证，则验证参数
            validateIfApplicable(binder, parameter);
            if (binder.getBindingResult().hasErrors()) {
                if (isBindExceptionRequired(binder, parameter)) {
                    throw new BindException(binder.getBindingResult());
                }
            }
        }

        // Add resolved attribute and BindingResult at the end of the model

        Map<String, Object> bindingResultModel = binder.getBindingResult().getModel();
        mavContainer.removeAttributes(bindingResultModel);
        mavContainer.addAllAttributes(bindingResultModel);

        return binder.getTarget();
    }
```
* 该方法的职责是实例化一个parameterType的对象，然后根据request和attribute、name创建一个WebDataBinder对象，其中。然后进入bindRequestParameters方法绑定，根据reqeust中的参数创建一个MutablePropertyValues对象。MutablePropertyValues里面存放了一个或多个PropertyValue，其中PropertyValue用于保存单个bean属性的相关信息，比如参数名、参数值。这里需要注意的是PropertyValue并不是保存request对象的所有参数属性信息。而是一个参数属性对应一个PropertyValue。比如这里的reqeust对象，携带了两个参数，name和age，便会分别创建两个PropertyValue对象。对应的MutablePropertyValues结构如下图
![](https://raw.githubusercontent.com/wycm/md-image/master/2018-02-11/2.jpg)
* 创建MutablePropertyValues对象化后，进入DataBinder.applyPropertyValues(DataBinder.java line737)。会根据刚刚创建的User对象。创建一个BeanWrapperImpl对象，BeanWrapperImpl实现了PropertyAccessor（属性访问器）接口。这是spring-bean下的一个类，在Sping中，对Bean属性的存取都是通过BeanWrapperImpl类来实现的。BeanWarapperImpl在这里作用就是通过PropertyValue中的属性相关描述，注入到BeanWarapperImpl对应的java对象的属性中去。具体注入的方法是setPropertyValues，这个方法略复杂。它的职责简单总结起来就是根据属性名调用对应的set...方法。比如注入User对象的name属性时，通过反射获取setName方法。如果有该方法便调用。这也是为什么在定义SpringMVC model 对象需要set...方法。如果没有set方法，参数注入便会失败。

### 参数解析绑定总结
1. SpringMVC初始化时，RequestMappingHandlerAdapter类会把一些默认的参数解析器添加到argumentResolvers中。当SpringMVC接收到请求后首先根据url查找对应的HandlerMethod。
2. 遍历HandlerMethod的MethodParameter数组
3. 根据MethodParameter的类型来查找确认使用哪个HandlerMethodArgumentResolver，遍历所有的argumentResolvers的supportsParameter(MethodParameter parameter)方法。。如果返回true，则表示查找成功，当前MethodParameter，使用该HandlerMethodArgumentResolver。这里确认大多都是根据参数的注解已经参数的Type来确认。
4. 解析参数，从request中解析出MethodParameter对应的参数，这里解析出来的结果都是String类型。
5. 转换参数，把对应String转换成具体方法所需要的类型，这里就包括了基本类型、对象、List、Set、Map。

## 总结
* 解析所使用代码已上传至github，https://github.com/wycm/SpringMVC-Demo
* 以上源码是基于SpringMVC 3.2.2.RELEASE版本。以上便是SpringMVC参数解析绑定的主要过程，希望对大家有帮助。本文可能有错误，希望读者能够指出来。
