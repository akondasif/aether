/**
 * 
 */
/**
 * @author roart
 *
 */
module cassandra {
    exports roart.database.cassandra;

    requires cassandra.driver.core;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires common.config;
    requires common.constants;
    requires common.database;
    requires common.model;
    requires guava;
    requires slf4j.api;
    requires spring.boot.autoconfigure;
    requires spring.cloud.commons;
    requires spring.web;
    requires spring.boot;
    requires database;
    //requires junit;
    //requires cassandra.unit;
    //requires cassandra.all;
    //requires libthrift;
}