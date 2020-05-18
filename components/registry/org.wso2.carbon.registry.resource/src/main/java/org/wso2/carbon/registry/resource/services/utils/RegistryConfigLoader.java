/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.registry.resource.services.utils;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.utils.CarbonUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class RegistryConfigLoader {
    private static Log log = LogFactory.getLog(RegistryConfigLoader.class);
    private static volatile RegistryConfigLoader registryConfigLoaderInstance = new RegistryConfigLoader();
    private boolean isUrlImportEnabled = true;

    private RegistryConfigLoader() {
        try {
            FileInputStream fileInputStream = new FileInputStream(getConfigFile());
            StAXOMBuilder builder = new StAXOMBuilder(
                    CarbonUtils.replaceSystemVariablesInXml(fileInputStream));
            OMElement configElement = builder.getDocumentElement();
            OMElement importConfig = configElement.getFirstChildWithName(
                    new QName("importResourceConfiguration"));
            if (importConfig != null) { //registry.xml need an <importResourceConfiguration> </importResourceConfiguration> entry to continue
                loadImportResourceConfiguration(importConfig);
            }

        } catch (FileNotFoundException e) {
            // This virtually cannot happen as registry.xml is necessary to start up the registry
            log.error("registry.xml has not been found", e);
        } catch (RegistryException e) {
            log.error(e.getMessage(),e);
        } catch (XMLStreamException e) {
            String msg = "error building registry.xml, check for badly formed xml";
            log.error(msg, e);
        } catch (CarbonException e) {
            log.error("An error occurred during system variable replacement", e);
        }
    }

    private void loadImportResourceConfiguration(OMElement urlImportConfig) {
        OMElement urlImport = urlImportConfig.getFirstChildWithName(new QName("urlImport"));
        if (urlImport != null) {
            try {
                isUrlImportEnabled = Boolean.parseBoolean(urlImport.getText());
            } catch (OMException e) {
                // we can use default value and continue if no OMElement found in urlImportConfig
                log.error("Error occurred when retrieving urlImport, hence using the default value '" +
                        isUrlImportEnabled + "'", e);
            }
        }
    }

    public static RegistryConfigLoader getInstance(){
        return registryConfigLoaderInstance;
    }

    // Get registry.xml instance.
    private static File getConfigFile() throws RegistryException {
        String configPath = CarbonUtils.getRegistryXMLPath();
        if (configPath != null) {
            File registryXML = new File(configPath);
            if (!registryXML.exists()) {
                String msg = "Registry configuration file (registry.xml) file does " +
                        "not exist in the path " + configPath;
                log.error(msg);
                throw new RegistryException(msg);
            }
            return registryXML;
        } else {
            String msg = "Cannot find registry.xml";
            log.error(msg);
            throw new RegistryException(msg);
        }
    }

    public boolean isUrlImportEnabled() {
        return isUrlImportEnabled;
    }
}
