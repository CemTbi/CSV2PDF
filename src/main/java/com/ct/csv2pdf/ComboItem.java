/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ct.csv2pdf;

/**
 *
 * @author Cem
 */
public class ComboItem {
    
    private String name;
    private String delimiter;

    public ComboItem(String name, String delimiter) {
        this.name = name;
        this.delimiter = delimiter;
    }

    public String getName() {
        return name;
    }

    public String getDelimiter() {
        return delimiter;
    }

    @Override
    public String toString() {
        return name;
    }
    
    
}
