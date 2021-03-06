/**
 * 
 */
/**
 * @author roart
 *
 */
module servicemanager.simple {
    exports roart.config;
    exports roart.controller;

    requires commons.lang;
    requires java.xml;
    requires slf4j.api;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.cloud.commons;
    requires spring.web;
    requires common.config;
    requires common.util;
    requires common.constants;
    requires common.service;
}