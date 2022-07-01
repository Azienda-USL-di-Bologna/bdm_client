package it.bologna.ausl.bdmclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.bdm.core.Bdm;
import it.bologna.ausl.bdm.core.BdmProcess;
import it.bologna.ausl.bdm.core.Step;
import it.bologna.ausl.bdm.core.Task;
import it.bologna.ausl.bdm.exception.BdmExeption;
import it.bologna.ausl.bdm.exception.IllegalStepStateException;
import it.bologna.ausl.bdm.exception.ProcessWorkFlowException;
import it.bologna.ausl.bdm.processmanager.BdmProcessManager;
import it.bologna.ausl.bdm.utilities.Bag;
import it.bologna.ausl.bdm.utilities.Dumpable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.logging.Level;
import javax.mail.URLName;
import javax.net.ssl.SSLContext;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author gdm
 */
public class RemoteBdmClientImplementation implements BdmClientInterface {

    private final String baseURL;
    private final RestTemplate restTemplate;
    private static final Logger log = Logger.getLogger(RemoteBdmClientImplementation.class);

    public RemoteBdmClientImplementation(String baseURL) {
        this.baseURL = baseURL;

        URLName url = new URLName(baseURL);
        restTemplate = new RestTemplate(setupHttpClient(url.getUsername(), url.getPassword(), true));

        List<HttpMessageConverter<?>> messageConverters = restTemplate.getMessageConverters();
        messageConverters.add(new BdmProcessHttpMessageConverter());

        MappingJackson2HttpMessageConverter jsonMessageConverter = (MappingJackson2HttpMessageConverter) messageConverters.stream().filter(c -> c.getClass() == MappingJackson2HttpMessageConverter.class).findFirst().get();
        ObjectMapper mapper = Dumpable.buildCustomObjectMapper();
        jsonMessageConverter.setObjectMapper(mapper);
    }

    /**
     * Imposta il connection factory per il client http, per usare basic
     * authentication e/o diverse strategie SSL
     *
     * @param username
     * @param password
     * @return
     */
    private ClientHttpRequestFactory setupHttpClient(String username, String password, boolean trustAnySSL) {

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();

        if (trustAnySSL) {
            SSLContext sslContext = null;
            try {
                KeyStore ks = KeyStore.getInstance("JKS");
                InputStream jks = Thread.currentThread().getContextClassLoader().getResourceAsStream("it/bologna/ausl/bdmclient/resources/star.internal.ausl.bologna.it.crt.jks");
//                ks.load(new FileInputStream("/Users/andrea/NetBeansProjects/bdm_client_2/star.internal.ausl.bologna.it.crt.jks"), "siamofreschi".toCharArray());
                ks.load(jks, "siamofreschi".toCharArray());
                //sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).useTLS().build();
                sslContext = SSLContexts.custom().loadTrustMaterial(ks).useTLS().build();
            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException ex) {
                log.error(ex);
            } catch (IOException | CertificateException ex) {
                java.util.logging.Logger.getLogger(RemoteBdmClientImplementation.class.getName()).log(Level.SEVERE, null, ex);
            }
            SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(sslContext, new AllowAllHostnameVerifier());

            clientBuilder.setSSLSocketFactory(sslConnectionFactory);

        }

        if (username != null && password != null) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }
        
        ClassLoader classLoader = RemoteBdmClientImplementation.class.getClassLoader();
        URL resource = classLoader.getResource("org/apache/http/message/BasicLineFormatter.class");
        System.out.println(resource);
        
        resource = classLoader.getResource("org/apache/http/impl/conn/SystemDefaultDnsResolver.class");
        System.out.println(resource);

        HttpClient httpClient = clientBuilder.build();
        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return requestFactory;
    }

    @Override
    public String startProcess(String processType, Bag context, Bag parameters) throws BdmExeption {
        // post su /bdmprocess per aggiungere il nuovo processo
        Bag additionalParamsBag = new Bag();
        additionalParamsBag.put(BdmProcessManager.ADDING_PROCESS_TYPE, processType);
        additionalParamsBag.put(BdmProcessManager.ADDING_PROCESS_PARAMS, context);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Bag> entity = new HttpEntity<>(additionalParamsBag, headers);

        BdmProcess bdmProcess = restTemplate.postForObject(baseURL + "bdmprocess/", entity, BdmProcess.class);

        // primo stepOn sul processo aggiunto
        if (stepOn(bdmProcess.getProcessId(), parameters) == Bdm.BdmStatus.ERROR) {
            throw new BdmExeption("errore nell'esecuzione del primo stepOn per l'avvio del processo");
        }
        return bdmProcess.getProcessId();
    }

    @Override
    public BdmProcess getProcess(String processId) {
        BdmProcess bdmProcess = restTemplate.getForObject(baseURL + "bdmprocess/" + processId, BdmProcess.class);
        return bdmProcess;
    }

    @Override
    public Step getCurrentStep(String processId) {
        return getProcess(processId).getCurrentStep();
    }

    @Override
    public Bdm.BdmStatus getProcessStatus(String processId) {
        return getProcess(processId).getStatus();
    }

    @Override
    public Task getCurrentTask(String processId) {
        return getProcess(processId).getCurrentTask();
    }

    @Override
    public List<String> getForwardSteps(String processId) {
        return getProcess(processId).getForwardSteps();
    }

    @Override
    public List<String> getBackwardSteps(String processId) {
        return getProcess(processId).getBackwardSteps();
    }

    @Override
    public Bag getContext(String processId) {
        return getProcess(processId).getContext();
    }

    @Override
    public void setContext(String processId, Bag context) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Bag> entity = new HttpEntity<>(context, headers);
        restTemplate.put(baseURL + "bdmprocess/" + processId + "/context/", entity);
    }
    
    @Override
    public void addInContext(String processId, Bag values) {
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Bag> entity = new HttpEntity<>(values, headers);
        restTemplate.put(baseURL + "bdmprocess/" + processId + "/context/patch", entity);
    }

    @Override
    public Bdm.BdmStatus stepOn(String processId, Bag parameters) throws IllegalStepStateException, ProcessWorkFlowException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Bag> entity = new HttpEntity<>(parameters, headers);
        restTemplate.put(baseURL + "bdmprocess/" + processId + "/stepon", entity);
        return getProcessStatus(processId);
    }

    @Override
    public Bdm.BdmStatus stepTo(String processId, String stepId, Bag parameters) throws IllegalStepStateException, ProcessWorkFlowException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Bag> entity = new HttpEntity<>(parameters, headers);
        restTemplate.put(baseURL + "bdmprocess/" + processId + "/stepto/" + stepId, entity);
        return getProcessStatus(processId);
    }

    @Override
    public Step getStep(String processId, String stepId) {
        Step step = restTemplate.getForObject(baseURL + "bdmprocess/" + processId + "/steps/" + stepId, Step.class);
        return step;
    }

    @Override
    public Step getStepByType(String processId, String stepType) {
        Step step = restTemplate.getForObject(baseURL + "bdmprocess/" + processId + "/steps/?stepType=" + stepType, Step.class);
        return step;
    }

    @Override
    public Step getNextStep(String processId, String stepId) {
        Step step = restTemplate.getForObject(baseURL + "bdmprocess/" + processId + "/steps/" + stepId + "/next", Step.class);
        return step;
    }

    @Override
    public String addTask(String taskType, Bag taskParameters, String processId, String stepId) throws BdmExeption {
        Bag parameters = new Bag();
        parameters.put("taskType", taskType);
        parameters.put("taskParameters", taskParameters);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Bag> entity = new HttpEntity<>(parameters, headers);
        String taskId = restTemplate.postForObject(baseURL + "bdmprocess/" + processId + "/steps/" + stepId + "/tasks/", entity, String.class);
        return taskId;
    }
    
    @Override
    public void removeTask(String processId, String stepId, String taskId) {
        restTemplate.delete(baseURL + "bdmprocess/" + processId + "/steps/" + stepId + "/tasks/" + taskId);    
    }
    
    @Override
    public void removeTasks(String processId, String stepId, List<String> taskIdList) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<String>> entity = new HttpEntity<>(taskIdList, headers);
        System.out.println(baseURL + "bdmprocess/" + processId + "/steps/" + stepId + "/tasks/remove/");
        restTemplate.postForObject(baseURL + "bdmprocess/" + processId + "/steps/" + stepId + "/tasks/remove/", entity, void.class);
    }

    @Override
    public String addStep(String processId, String stepDescription, Step.StepLogic stepLogic, String stepType, List<Step.StepLogic> allowedStepLogic) throws BdmExeption {
        Bag parameters = new Bag();
        parameters.put("stepDescription", stepDescription);
        parameters.put("stepLogic", stepLogic);
        parameters.put("stepType", stepType);
        parameters.put("allowedStepLogic", allowedStepLogic);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Bag> entity = new HttpEntity<>(parameters, headers);
        String strepId = restTemplate.postForObject(baseURL + "bdmprocess/" + processId + "/steps/", entity, String.class);
        return strepId;
    }
    
    @Override
    public void removeStep(String processId, String stepId) {
        restTemplate.delete(baseURL + "bdmprocess/" + processId + "/steps/" + stepId);    
    }

    @Override
    public void setStepLogic(String processId, String stepId, Step.StepLogic stepLogic) throws BdmExeption {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Step.StepLogic> entity = new HttpEntity<>(stepLogic, headers);
        restTemplate.put(baseURL + "bdmprocess/" + processId + "/steps/" + stepId, entity);
    }

    public String[] getProcessList() {
        String[] res;
        res = restTemplate.getForObject(baseURL + "bdmprocess/", String[].class);
        return res;
    }

    @Override
    public void abortProcess(String processId) {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        HttpEntity<> entity = new HttpEsentity<>();
        restTemplate.put(baseURL + "bdmprocess/" + processId + "/abort", null);
    }

    @Override
    public void deleteProcess(String processId) {
        restTemplate.delete(baseURL + "bdmprocess/" + processId);    
    }

}
