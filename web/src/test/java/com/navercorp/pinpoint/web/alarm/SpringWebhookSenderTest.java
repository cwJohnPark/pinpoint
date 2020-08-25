package com.navercorp.pinpoint.web.alarm;

import com.navercorp.pinpoint.web.alarm.checker.AgentChecker;
import com.navercorp.pinpoint.web.alarm.checker.AlarmChecker;
import com.navercorp.pinpoint.web.alarm.checker.SlowCountToCalleeChecker;
import com.navercorp.pinpoint.web.alarm.checker.SystemCpuUsageRateChecker;
import com.navercorp.pinpoint.web.alarm.vo.AgentCheckerValue;
import com.navercorp.pinpoint.web.alarm.vo.AlarmCheckerValue;
import com.navercorp.pinpoint.web.alarm.vo.Rule;
import com.navercorp.pinpoint.web.alarm.vo.WebhookPayload;
import com.navercorp.pinpoint.web.batch.BatchConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;

public class SpringWebhookSenderTest {
    
    SpringWebhookSender sender;
    
    @Test
    public void constructorRequiresNotNullTest() throws Exception {
        try {
            new SpringWebhookSender(null, new RestTemplate());
            fail();
        } catch(NullPointerException npe) {
            // pass
        }
        try {
            new SpringWebhookSender(new BatchConfiguration(), null);
            fail();
        } catch (NullPointerException npe) {
            // pass
        }
        try {
            new SpringWebhookSender(null, null);
            fail();
        } catch (NullPointerException npe) {
            // pass
        }
    }
    
    @Test
    public void whenWebhookEnableFalseDoNotTriggerWebhook() throws Exception {
        // given
        BatchConfiguration configurationStub = getConfigurationStub(false);
        RestTemplate restTemplateStub = getRestTemplateStub();
        sender = new SpringWebhookSender(configurationStub, restTemplateStub);
        
        // when
        sender.triggerWebhook(null, 0, null);
        
        // then
        verify(restTemplateStub, never())
                .exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class));
    }
    
    @Test
    public void whenWebhookEnableTrueDoTriggerWebhook() throws Exception {
        // given
        BatchConfiguration configurationStub = getConfigurationStub(true);
        RestTemplate restTemplateStub = getRestTemplateStub();
        sender = new SpringWebhookSender(configurationStub, restTemplateStub);
        
        // when
        sender.triggerWebhook(getAlarmCheckerStub(), 0, null);
        
        // then
        verify(restTemplateStub, times(1))
                .exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class));
        
    }
    
    @Test
    public void whenSendAgentCheckerCheckerTypeIsAgentChecker() throws Exception {
        // given
        BatchConfiguration configurationStub = getConfigurationStub(true);
        RestTemplate restTemplateStub = getRestTemplateStub();
        sender = new SpringWebhookSender(configurationStub, restTemplateStub);
    
        ArgumentCaptor<HttpEntity<WebhookPayload>> argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        
        // when
        sender.triggerWebhook(getAgentCheckerStub(), 0, null);
        verify(restTemplateStub)
                .exchange(any(URI.class), any(HttpMethod.class), argumentCaptor.capture(), any(Class.class));
        
        // then
        HttpHeaders headers = argumentCaptor.getValue().getHeaders();
        assertThat(headers.get("Checker-Type").get(0), is("AgentChecker"));
    }
    
    @Test
    public void whenSendAlarmCheckerCheckerTypeIsAlarmChecker() throws Exception {
        // given
        BatchConfiguration configurationStub = getConfigurationStub(true);
        RestTemplate restTemplateStub = getRestTemplateStub();
        sender = new SpringWebhookSender(configurationStub, restTemplateStub);
    
        ArgumentCaptor<HttpEntity<WebhookPayload>> argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    
        // when
        sender.triggerWebhook(getAlarmCheckerStub(), 0, null);
        verify(restTemplateStub)
                .exchange(any(URI.class), any(HttpMethod.class), argumentCaptor.capture(), any(Class.class));
    
        // then
        HttpHeaders headers = argumentCaptor.getValue().getHeaders();
        assertThat(headers.get("Checker-Type").get(0), is("AlarmChecker"));
    }
    
    private RestTemplate getRestTemplateStub() {
        RestTemplate restTemplateMock = mock(RestTemplate.class);
        doReturn(ResponseEntity.of(Optional.of("success")))
                .when(restTemplateMock)
                .exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class));
        return restTemplateMock;
    }
    
    private BatchConfiguration getConfigurationStub(boolean webhookEnable) {
        BatchConfiguration batchConfigurationMock = mock(BatchConfiguration.class);
        
        when(batchConfigurationMock.getWebhookEnable())
                .thenReturn(webhookEnable);
        when(batchConfigurationMock.getWebhookReceiverUrl())
                .thenReturn("test-url");
        when(batchConfigurationMock.getPinpointUrl())
                .thenReturn("pinpoint-url");
        when(batchConfigurationMock.getBatchEnv())
                .thenReturn("batch-env");
        
        
        return batchConfigurationMock;
    }
    
    private AlarmChecker<Long> getAlarmCheckerStub() {
        SlowCountToCalleeChecker checker = mock(SlowCountToCalleeChecker.class);
        doReturn(new AlarmCheckerValue<Long>("unit", 1000L)).when(checker).getCheckerValue();
        doReturn(new Rule("app-id", "server-type", "checker-name", 0, "usergroup-id", true, true, "notes"))
                .when(checker).getRule();
        return checker;
    }
    
    private AgentChecker<Long> getAgentCheckerStub() {
        SystemCpuUsageRateChecker checker = mock(SystemCpuUsageRateChecker.class);
        Map<String, Long> map = new HashMap<>();
        map.put("key1", 1000L);
        doReturn(new AgentCheckerValue<Long>("unit", map)).when(checker).getCheckerValue();
        doReturn(new Rule("app-id", "server-type", "checker-name", 0, "usergroup-id", true, true, "notes"))
                .when(checker).getRule();
        return checker;
    }
}