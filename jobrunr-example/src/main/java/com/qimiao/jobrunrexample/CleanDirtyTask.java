package com.qimiao.jobrunrexample;

import org.springframework.stereotype.Service;

@Service
public class CleanDirtyTask {

    public void clean() {
        System.out.println("I am cleaning data.");
    }
}

