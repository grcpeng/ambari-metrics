/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.metrics.core.loadsimulator.util;

import java.io.IOException;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

/**
 * Small wrapper that configures the ObjectMapper with some defaults.
 */
public class Json {
  private ObjectMapper myObjectMapper;

  /**
   * Creates default Json ObjectMapper that maps fields.
   */
  public Json() {
    this(false);
  }

  /**
   * Creates a Json ObjectMapper that maps fields and optionally pretty prints the
   * serialized objects.
   * 创建一个Json ObjectMapper，它可以映射字段，并可以选择漂亮地打印序列化的对象。
   * @param pretty a flag - if true the output will be pretty printed.
   */
  public Json(boolean pretty) {
    myObjectMapper = new ObjectMapper();
    myObjectMapper.setVisibility(JsonMethod.FIELD, JsonAutoDetect.Visibility.ANY);
    if (pretty) {
    	// 允许使用默认漂亮打印机为基础生成器启用(或禁用)缩进的特性(详细信息请参阅JsonGenerator.useDefaultPrettyPrinter()))。
	    // 注意，这只影响ObjectMapper隐式构造JsonGenerator的情况:如果传递显式生成器，则不会更改其配置。
	    // 还要注意，如果要配置缩进的详细信息，则需要直接配置生成器:有一个方法可以使用任何漂亮的打印机实例。这个特性只允许使用默认实现。
      myObjectMapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
    }
  }

	/**
	 * 用于将任何Java值序列化为字符串的方法。
	 * 与writeValue(Writer,Object)在功能上等效，但writeValueAsString(Object  value)更高效。
	 * @param o
	 * @return
	 * @throws IOException
	 */
  public String serialize(Object o) throws IOException {
    return myObjectMapper.writeValueAsString(o);
  }

	/**
	 * 将字符串内容反序列化为类对象
	 * @param content
	 * @param paramClass
	 * @param <T>
	 * @return
	 * @throws IOException
	 */
  public <T> T deserialize(String content, Class<T> paramClass) throws IOException {
    return myObjectMapper.readValue(content, paramClass);
  }

}
