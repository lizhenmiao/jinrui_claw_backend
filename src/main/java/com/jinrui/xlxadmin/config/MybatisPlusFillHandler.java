package com.jinrui.xlxadmin.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

@Component
public class MybatisPlusFillHandler implements MetaObjectHandler {
  @Override
  public void insertFill(MetaObject metaObject) {
    LocalDateTime now = LocalDateTime.now();
    strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
    strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
    strictInsertFill(metaObject, "firstSeenAt", LocalDateTime.class, now);
    strictInsertFill(metaObject, "lastSeenAt", LocalDateTime.class, now);
  }

  @Override
  public void updateFill(MetaObject metaObject) {
    LocalDateTime now = LocalDateTime.now();
    strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, now);
    strictUpdateFill(metaObject, "lastSeenAt", LocalDateTime.class, now);
  }
}
