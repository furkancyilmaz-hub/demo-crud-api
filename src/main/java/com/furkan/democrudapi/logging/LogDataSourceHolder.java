package com.furkan.democrudapi.logging;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class LogDataSourceHolder {

    private static volatile DataSource dataSource;

    public LogDataSourceHolder(DataSource dataSource) {
        LogDataSourceHolder.dataSource = dataSource;
    }

    static DataSource get() {
        return dataSource;
    }
}