package com.bis.constants;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author yuri.lisnovsky
 */
public enum CreativeStatusEnum {

    NOT_FOUND(0), FOUND(1);

    // Internal state
    private int creativeStatus;

    // Constructor
    private CreativeStatusEnum(final int creativeStatus) {
        this.creativeStatus = creativeStatus;
    }

    public int getCreativeStatus() {
        return creativeStatus;
    }    
}
