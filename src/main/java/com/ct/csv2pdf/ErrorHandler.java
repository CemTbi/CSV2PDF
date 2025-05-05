/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ct.csv2pdf;

import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author Cem
 */
public class ErrorHandler {
    // Shows an Error
    public static void showUserErrorMessage(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // Shows success
    public static void showUserSuccessMessage(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    // Protocol of errors for the dev
    public static void logError(Exception ex, String message) {
        Logger.getLogger(ErrorHandler.class.getName()).log(Level.SEVERE, message, ex);
    }

    // Shows warning
    public static void showUserWarningMessage(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }
}
