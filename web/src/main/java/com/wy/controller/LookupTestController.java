package com.wy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/LookupTest")
public class LookupTestController {

    @RequestMapping(value = "/test1", method = RequestMethod.GET)
    @ResponseBody
    public String test1(){
        return "method test1";
    }

    @RequestMapping(value = "/test1", headers = "Referer=https://www.baidu.com")
    @ResponseBody
    public String test2(){
        return "method test2";
    }

    @RequestMapping(value = "/test1", params = "id=1")
    @ResponseBody
    public String test3(){
        return "method test3";
    }

    @RequestMapping(value = "/*")
    @ResponseBody
    public String test4(){
        return "method test4";
    }

    @RequestMapping(value = "/test5", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public String test5(){
        return "method test5";
    }

    @RequestMapping(value = "/test5", method = {RequestMethod.GET, RequestMethod.DELETE})
    @ResponseBody
    public String test6(){
        return "method test6";
    }
}
