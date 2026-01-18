package com.iei.api_cv.service;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class CoordenadasService {

    public static double[] obtenerLatLon(String direccion) {

        WebDriver driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        try {
            driver.get("https://www.coordenadas-gps.com/convertidor-de-coordenadas-gps");
            try {
                By botonCookies = By.xpath(
                        "//*[contains(text(),'Consent') or " +
                                "contains(text(),'Aceptar') or " +
                                "contains(text(),'Accept') or " +
                                "contains(text(),'OK')]"
                );

                WebElement btn = new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.elementToBeClickable(botonCookies));

                btn.click();
            } catch (Exception ignored) {
                System.out.println("⚠ No se encontró banner de cookies, continuando...");
            }
            WebElement input = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.id("address"))
            );
            input.clear();
            input.sendKeys(direccion + ", Comunidad Valenciana, España");

            ((JavascriptExecutor) driver).executeScript("codeAddress();");

            wait.until(d -> !d.findElement(By.id("latitude")).getAttribute("value").isEmpty());
            wait.until(d -> !d.findElement(By.id("longitude")).getAttribute("value").isEmpty());

            WebElement lat = driver.findElement(By.id("latitude"));
            WebElement lon = driver.findElement(By.id("longitude"));

            return new double[]{
                    Double.parseDouble(lat.getAttribute("value")),
                    Double.parseDouble(lon.getAttribute("value"))
            };

        } catch (Exception e) {
            System.out.println("Error obteniendo coordenadas para: " + direccion);
            return new double[]{0.0, 0.0};
        } finally {
            driver.quit();
        }
    }
}
