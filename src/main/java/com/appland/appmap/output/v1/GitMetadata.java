package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;

public class GitMetadata {
  public String branch;
  public String commit;
  public String status;
  public String tag;

  @JSONField(name = "annotated_tag")
  public String annotatedTag;

  @JSONField(name = "commits_since_tag")
  public String commitsSinceTag;

  @JSONField(name = "commits_since_annotated_tag")
  public String commitsSinceAnnotatedTag;
}