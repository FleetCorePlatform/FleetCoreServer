package io.fleetcoreplatform.Managers.IoTCore;

public class IotPolicyMaker {
    public static String buildPolicyDocument(String accountId, String region) {
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
                  }
                ]
              }
            """;

        return template.replace("${region}", region).replace("${accountId}", accountId);
    }
}
