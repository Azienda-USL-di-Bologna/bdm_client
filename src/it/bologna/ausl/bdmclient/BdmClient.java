/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.bdmclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.bologna.ausl.bdm.core.BdmProcess;
import it.bologna.ausl.bdm.utilities.Bag;
import it.bologna.ausl.bdm.utilities.Dumpable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author andrea
 */
public class BdmClient {

    private static final String baseURL = "http://localhost:8080/";

    public static void main(String args[]) throws JsonProcessingException, IOException, ClassNotFoundException {
        RestTemplate restTemplate = new RestTemplate();
        List<HttpMessageConverter<?>> messageConverters = restTemplate.getMessageConverters();
        messageConverters.add(0, new BdmProcessHttpMessageConverter());

        ArrayList<String> processIds = restTemplate.getForObject(baseURL + "bdmprocess/", ArrayList.class);
        for (String s : processIds) {
            System.out.println(s);
        }

        String processId = processIds.get(0);
        BdmProcess p = restTemplate.getForObject(baseURL + "bdmprocess/f641446f-a61a-45de-9ef2-d859e5f1c9e5", BdmProcess.class);

        if (p != null) {
            System.out.println(p.dump());
        }

        Bag b = new Bag();
        b.put("processType", "SampleProcess");
        String processS = restTemplate.postForObject(baseURL + "bdmprocess/", b, String.class);

        BdmProcess process = Dumpable.load(processS, BdmProcess.class);

        restTemplate.put(baseURL + "bdmprocess/" + process.getProcessId() + "/stepon", new Bag());

        processS = restTemplate.getForObject(baseURL + "bdmprocess/" + process.getProcessId(), String.class);
        System.out.println(Dumpable.load(processS, BdmProcess.class).getStatus());

    }

}
