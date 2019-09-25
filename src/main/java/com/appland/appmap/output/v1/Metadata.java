package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;

public class Metadata {
  public String name;
  public String repository;
  public String[] labels;
  public String layout;
  public String app;
  public String feature;
  public LanguageMetadata language;
  public FrameworkMetadata[] frameworks;
  public GitMetadata git;

  @JSONField(name = "static")
  public String layoutOwner;

  @JSONField(name = "app_owner")
  public String appOwner;

  @JSONField(name = "feature_group")
  public String featureGroup;
}