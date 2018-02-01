//package com.wy.controller;
//
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestMethod;
//
///**
// * 源码分析Controller
// */
//@Controller
//@RequestMapping("/springMVC")
//public class SpringMVCSourceController {
//    @RequestMapping(value = "/methodTest")
//    public String test1(){
//        return "index";
//    }
//    @RequestMapping
//    public String test10(){
//        return "index";
//    }
//    @RequestMapping(value = "/methodTest/*.do")
//    public String test121(){
//        return "index";
//    }
//    @RequestMapping(value = "/methodTest/*")
//    public String test131(){
//        return "index";
//    }
//    @RequestMapping(value = "/methodTest", params = "id")
//    public String test11(){
//        System.out.println("enter test11");
//        return "index";
//    }
//    @RequestMapping(value = "/methodTest", params = "idd")
//    public String test111(){
//        System.out.println("enter test111");
//        return "index";
//    }
//    @RequestMapping(value = "/methodTest", params = {"id", "name"})
//    public String test12(){
//        System.out.println("enter test12");
//        return "index";
//    }
//    @RequestMapping(value = "/methodTest", method = RequestMethod.POST)
//    public String test2(){
//        System.out.println("enter test2");
//        return "index";
//    }
//
//    @RequestMapping(value = "/methodTest", method = RequestMethod.GET)
//    public String test3(){
//        System.out.println("enter method test3");
//        return "index";
//    }
//
//    @RequestMapping(value = "/methodTest", method = RequestMethod.GET, headers = "User-Agent")
//    public String test4(){
//        System.out.println("enter method test4");
//        return "index";
//    }
//
//    @RequestMapping("/pathVariableTest/{string}")
//    public String test5(@PathVariable String string){
//        System.out.println(string);
//        return "index";
//    }
//
//}
