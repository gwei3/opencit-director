/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.mtwilson.director.javafx.utils;

/**
 *
 * @author boskisha
 */
public class UnsuccessfulRemoteMountException extends RuntimeException {

    public UnsuccessfulRemoteMountException() {
        super();
    }

    public UnsuccessfulRemoteMountException(Throwable e) {
        super(e);
    }

    public UnsuccessfulRemoteMountException(String message) {
        super(message);
    }

    public UnsuccessfulRemoteMountException(String message, Throwable e) {
        super(message, e);
    }
}