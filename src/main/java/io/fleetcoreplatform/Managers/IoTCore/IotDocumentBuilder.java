package io.fleetcoreplatform.Managers.IoTCore;

import io.fleetcoreplatform.Managers.IoTCore.Enums.MissionDocumentEnums;

public class IotDocumentBuilder {
    public static String buildJobDocument(MissionDocumentEnums action, String missionName, String downloadUrl, String filePath, String outpost, String group, String bucket) {
         String template =
                """
            {
              "operation": "${action}",
              "data": {
                "mission_uuid": "${missionName}",
                "download_url": "${downloadUrl}",
                "download_path": "${filePath}",
                "metadata": {
                  "outpost": "${outpost}",
                  "group": "${group}",
                  "bucket": "${bucket}"
                }
              }
            }
            """;

        return template
                .replace("${action}", action.toString())
                .replace("${missionName}", missionName)
                .replace("${downloadUrl}", downloadUrl)
                .replace("${filePath}", filePath)
                .replace("${outpost}", outpost)
                .replace("${group}", group)
                .replace("${bucket}", bucket);
    }

    public static String buildPolicyDocument(String accountId, String region, String roleAlias) {
        // TODO: Fix policy template that does not grant sufficient permissions (#20)
        String template =
                """
            {
                "Version": "2012-10-17",
                "Statement": [
                  {
                    "Effect": "Allow",
                    "Action": "iot:Connect",
                    "Resource": "arn:aws:iot:${region}:${accountId}:client/${iot:Connection.Thing.ThingName}"
                  },
                  {
                    "Effect": "Allow",
                    "Action": "iot:Publish",
                    "Resource": "arn:aws:iot:${region}:${accountId}:topic/devices/${iot:Connection.Thing.ThingName}/telemetry"
                  },
                  {
                    "Effect": "Allow",
                    "Action": "iot:Subscribe",
                    "Resource": [
                      "arn:aws:iot:${region}:${accountId}:topicfilter/groups/${iot:Connection.Thing.ThingName}/cancel",
                      "arn:aws:iot:${region}:${accountId}:topicfilter/$aws/things/${iot:Connection.Thing.ThingName}/jobs/notify"
                    ]
                  },
                  {
                    "Effect": "Allow",
                    "Action": "iot:Receive",
                    "Resource": [
                      "arn:aws:iot:${region}:${accountId}:topic/groups/${iot:Connection.Thing.ThingName}/cancel",
                      "arn:aws:iot:${region}:${accountId}:topic/$aws/things/${iot:Connection.Thing.ThingName}/jobs/notify"
                    ]
                  },
                  {
                    "Effect": "Allow",
                    "Action": [
                      "iotjobsdata:DescribeJobExecution",
                      "iotjobsdata:GetPendingJobExecutions",
                      "iotjobsdata:UpdateJobExecution",
                      "iotjobsdata:StartNextPendingJobExecution"
                    ],
                    "Resource": "arn:aws:iot:${region}:${accountId}:thing/${iot:Connection.Thing.ThingName}"
                  },
                  {
                    "Effect": "Allow",
                    "Action": "iot:AssumeRoleWithCertificate",
                    "Resource": "arn:aws:iot:${region}:${accountId}:rolealias/${roleAlias}"
                  }
                ]
              }
            """;

        return template.replace("${region}", region).replace("${accountId}", accountId).replace("${roleAlias}", roleAlias);
    }
}
