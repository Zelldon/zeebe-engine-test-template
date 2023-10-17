/*
 * Copyright © 2021 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.camunda.zeebe.process.test.examples;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.extension.ZeebeProcessTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@ZeebeProcessTest
public class PullRequestProcessTest {

  // injected by ZeebeProcessTest annotation
  private ZeebeTestEngine engine;
  // injected by ZeebeProcessTest annotation
  private ZeebeClient client;

  private static final BpmnModelInstance process =
          Bpmn.createExecutableProcess("process")
                  .startEvent()
                  .parallelGateway("gw")
                  .serviceTask("task")
                  .zeebeJobType("type")
                  .endEvent()
                  .moveToLastGateway()
                  .serviceTask("incidentTask")
                  .zeebeInputExpression("=foo", "bar")
                  .zeebeJobType("type")
                  .endEvent()
                  .done();

  @BeforeEach
  void deployProcess() {
    client.newDeployCommand().addProcessModel(process, "process.bpmn").send().join();
  }

  @Test
  void shouldRunIntoAnIncident() throws InterruptedException, TimeoutException {
    // given
    // when
    final var returnedProcessInstance = client
            .newCreateInstanceCommand()
            .bpmnProcessId("process")
            .latestVersion()
            .variables(Map.of("var1", "1", "var2", "12", "var3", "123"))
            .send()
            .join();

    // then
    engine.waitForIdleState(Duration.ofSeconds(5));
    BpmnAssert.assertThat(returnedProcessInstance)
            .hasAnyIncidents()
            ;
  }
}
