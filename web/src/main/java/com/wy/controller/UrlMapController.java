package com.wy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/UrlMap")
public class UrlMapController {

    @RequestMapping(value = "/test1", method = RequestMethod.GET)
    @ResponseBody
    public String test1(HttpServletRequest request, String id){
        return "method test1";
    }

    @RequestMapping(value = "/test1")
    @ResponseBody
    public String test2(){
        return "method test2";
    }

    @RequestMapping(value = "/test3")
    @ResponseBody
    public String test3(){
        return "method test3";
    }

    public String test4(){
        return "method test4";
    }
}
