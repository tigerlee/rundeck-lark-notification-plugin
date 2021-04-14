/*
 * MIT License
 *
 * Copyright (c) 2018 Tiger Lee
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.tigerlee.LarkNotificationPlugin;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.descriptions.TextArea;
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption;
import static com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants.DISPLAY_TYPE_KEY;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Plugin(service = "Notification", name = "LarkNotificationPlugin")
@PluginDescription(title = "飞书", description = "通过自定义机器人发送Rundeck通知")
public class LarkNotificationPlugin implements NotificationPlugin {
  private static final Configuration FREEMARKER_CFG = new Configuration();
  private static final String WECHAT_MESSAGE_TEMPLATE = "lark-incoming-message.ftl";

  @PluginProperty(name = "webhookUrl", title = "Webhook地址",
      description = "飞书自定义机器人Webhook地址。可以添加多个地址，以逗号分隔。", required = true)
  private String webhookUrl;

  @PluginProperty(name = "mentions", title = "＠", description = "需要提及的人。", required = false)
  private String mentions;

  public LarkNotificationPlugin() {
    mentions = "";
  }

  private class LarkNotificationPluginException extends Exception {
    private LarkNotificationPluginException(String message) {
      super(message);
    }
  }

  private String sendMessage(String theWebhookUrl, String theFormattedMessage)
      throws IOException, LarkNotificationPluginException {
    HttpURLConnection connectionToWebhook =
        (HttpURLConnection) new URL(theWebhookUrl).openConnection();

    connectionToWebhook.setConnectTimeout(5000);
    connectionToWebhook.setReadTimeout(5000);
    connectionToWebhook.setRequestMethod("POST");
    connectionToWebhook.setRequestProperty("Content-type", "application/json");
    connectionToWebhook.setDoOutput(true);

    DataOutputStream bodyOfRequest = new DataOutputStream(connectionToWebhook.getOutputStream());
    bodyOfRequest.write(theFormattedMessage.getBytes("UTF-8"));
    bodyOfRequest.flush();
    bodyOfRequest.close();

    int responseCode = connectionToWebhook.getResponseCode();
    connectionToWebhook.disconnect();

    String result = "The response code is: " + responseCode;

    if (responseCode != 200) {
      throw new LarkNotificationPluginException(result);
    }

    return result;
  }

  private String generateMessage(String trigger, Map executionData, Map config)
      throws LarkNotificationPluginException {

    HashMap<String, Object> model = new HashMap<String, Object>();
    model.put("trigger", trigger);
    model.put("executionData", executionData);
    model.put("config", config);
    model.put("mentions", mentions);
    StringWriter sw = new StringWriter();
    try {
      Template template = FREEMARKER_CFG.getTemplate(WECHAT_MESSAGE_TEMPLATE);
      template.process(model, sw);

    } catch (IOException ioEx) {
      throw new LarkNotificationPluginException(
          "Error loading Lark message template: [" + ioEx.getMessage() + "].");
    } catch (TemplateException templateEx) {
      throw new LarkNotificationPluginException(
          "Error merging Lark notification message template: [" + templateEx.getMessage() + "].");
    }

    return sw.toString();
  }

  public boolean postNotification(String trigger, Map executionData, Map config) {
    ClassTemplateLoader builtInTemplate =
        new ClassTemplateLoader(LarkNotificationPlugin.class, "/templates");
    TemplateLoader[] loaders = new TemplateLoader[] {builtInTemplate};
    MultiTemplateLoader mtl = new MultiTemplateLoader(loaders);
    FREEMARKER_CFG.setTemplateLoader(mtl);

    try {
      String message = generateMessage(trigger, executionData, config);
      for (String url : webhookUrl.split(",")) {
        sendMessage(url, message);
      }
    } catch (SecurityException | IllegalArgumentException | LarkNotificationPluginException
        | IOException e) {
      e.printStackTrace();
    }

    return true;
  }
}
