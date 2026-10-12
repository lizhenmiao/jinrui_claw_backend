package com.jinrui.xlxadmin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.jinrui.xlxadmin.mapper")
@SpringBootApplication
public class XlxAdminApplication {
  public static void main(String[] args) {
    SpringApplication.run(XlxAdminApplication.class, args);
  }
}
