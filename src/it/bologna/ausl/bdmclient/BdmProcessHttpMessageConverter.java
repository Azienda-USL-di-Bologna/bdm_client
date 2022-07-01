/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.bdmclient;

import it.bologna.ausl.bdm.core.BdmProcess;
import it.bologna.ausl.bdm.utilities.Dumpable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 *
 * @author andrea
 */
public class BdmProcessHttpMessageConverter implements HttpMessageConverter<BdmProcess> {

    @Override
    public boolean canRead(Class<?> type, MediaType mt) {
        if (type.equals(BdmProcess.class) && mt != null && mt.getType().equals("application") && mt.getSubtype().equals("json")) {
            return true;
        }
////        if (type.isInstance(SampleProcess.class) && mt.equals(MediaType.APPLICATION_JSON)) {
////            return true;
////        }
        return false;
    }

    @Override
    public boolean canWrite(Class<?> type, MediaType mt) {
        return canRead(type, mt);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_JSON);//To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public BdmProcess read(Class<? extends BdmProcess> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        try {
            return Dumpable.loadFromInputStream(him.getBody(), type);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(BdmClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public void write(BdmProcess t, MediaType mt, HttpOutputMessage hom) throws IOException, HttpMessageNotWritableException {
        t.dumpToOutputStream((OutputStream) hom);
    }

}
