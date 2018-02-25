package com.alvinhkh.buseta.model;

import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

/*
 * https://stackoverflow.com/a/17220939/2411672
 */
public class EmptyElementConverter implements Converter<String> {
    @Override
    public String read(InputNode node) throws Exception
    {
        /* Implement if required */
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void write(OutputNode node, String value) throws Exception
    {
        /* Simple implementation: do nothing here ;-) */
    }
}