<#if executionData.status == "succeeded">
    <#assign color="green">
<#elseif executionData.status == "failed">
    <#assign color="red">
<#elseif executionData.status == "aborted">
    <#assign color="orange">
<#else>
    <#assign color="blue">
</#if>

<#assign startTime="${executionData.dateStarted?string[\"yyyy-MM-dd HH:mm:ss\"]}">

<#if executionData.dateEnded??>
<#assign endTime="${executionData.dateEnded?string[\"yyyy-MM-dd HH:mm:ss\"]}">
<#else>
<#assign endTime="">
</#if>

{
  "msg_type": "interactive",
  "card": {
    "header": {
      "title": {
        "tag": "plain_text",
        "content": "${executionData.job.name}(#${executionData.id})"
      },
      "template": "${color}"
    },
    "elements": [
      {
        "tag": "div",
        "fields": [
          {
            "is_short": false,
            "text": {
              "tag": "lark_md",
              "content": "**状态:** ${executionData.status}"
            }
          },
          {
            "is_short": true,
            "text": {
              "tag": "lark_md",
              "content": "**项目:** ${executionData.job.project}"
            }
          },
          {
            "is_short": true,
            "text": {
              "tag": "lark_md",
              "content": "**组名:** ${executionData.job.group}"
            }
          },
          {
            "is_short": true,
            "text": {
              "tag": "lark_md",
              "content": "**开始:** ${startTime}"
            }
          },
          {
            "is_short": true,
            "text": {
              "tag": "lark_md",
              "content": "**结束:** ${endTime}"
            }
          },
          {
            "is_short": true,
            "text": {
              "tag": "lark_md",
              "content": "**用户:** ${executionData.user}"
            }
          },
          {
            "is_short": true,
            "text": {
              "tag": "lark_md",
              "content": "**提及:** ${mentions}"
            }
          }
        ]
      },
      {
        "tag": "action",
        "actions": [
          {
            "tag": "button",
            "text": {
              "tag": "plain_text",
              "content": "查看详情"
            },
            "url": "${executionData.href}",
            "type": "default"
          }
        ]
      }
    ]
  }
}
