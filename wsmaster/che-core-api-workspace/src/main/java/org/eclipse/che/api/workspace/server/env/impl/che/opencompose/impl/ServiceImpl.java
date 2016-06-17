/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.workspace.server.env.impl.che.opencompose.impl;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.eclipse.che.api.workspace.server.env.impl.che.opencompose.model.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Alexander Garagatyi
 */
public class ServiceImpl implements Service {
    @JsonProperty("container_name")
    private String              containerName;
    private String              context;
    private String              dockerfile;
    private List<String>        command;
    private List<String>        entrypoint;
    private String              image;
    @JsonProperty("depends_on")
    private List<String>        dependsOn;
    private Map<String, String> environment;
    private List<String>        expose;
    private List<String>        ports;
    private Map<String, String> labels;
    private List<String>        links;
    private List<String>        volumes;
    @JsonProperty("volumes_from")
    private List<String>        volumesFrom;
    // todo missing in the model
    @JsonProperty("mem_limit")
    private Integer             memLimit;
    //todo env_file list
    //todo extends

    public ServiceImpl() {}

    public ServiceImpl(Service service) {
        image = service.getImage();
        context = service.getContext();
        dockerfile = service.getDockerfile();
        entrypoint = service.getEntrypoint();
        command = service.getCommand();
        environment = service.getEnvironment();
        dependsOn = service.getDependsOn();
        containerName = service.getContainerName();
        links = service.getLinks();
        labels = service.getLabels();
        expose = service.getExpose();
        ports = service.getPorts();
        volumesFrom = service.getVolumesFrom();
        volumes = service.getVolumes();
        memLimit = service.getMemLimit();
    }

    @Override
    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @Override
    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    @Override
    public String getDockerfile() {
        return dockerfile;
    }

    public void setDockerfile(String dockerfile) {
        this.dockerfile = dockerfile;
    }

    @Override
    public List<String> getEntrypoint() {
        if (entrypoint == null) {
            entrypoint = new ArrayList<>();
        }
        return entrypoint;
    }

    public void setEntrypoint(List<String> entrypoint) {
        this.entrypoint = entrypoint;
    }

    @Override
    public List<String> getCommand() {
        if (command == null) {
            command = new ArrayList<>();
        }
        return command;
    }

    public void setCommand(List<String> command) {
        this.command = command;
    }

    @Override
    public Map<String, String> getEnvironment() {
        if (environment == null) {
            environment = new HashMap<>();
        }
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    @Override
    public List<String> getDependsOn() {
        if (dependsOn == null) {
            dependsOn = new ArrayList<>();
        }

        return dependsOn;
    }

    public void setDependsOn(List<String> dependsOn) {
        this.dependsOn = dependsOn;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    @Override
    public List<String> getLinks() {
        if (links == null) {
            links = new ArrayList<>();
        }
        return links;
    }

    public void setLinks(List<String> links) {
        this.links = links;
    }

    @Override
    public Map<String, String> getLabels() {
        if (labels == null) {
            labels = new HashMap<>();
        }
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    @Override
    public List<String> getExpose() {
        if (expose == null) {
            expose = new ArrayList<>();
        }
        return expose;
    }

    public void setExpose(List<String> expose) {
        this.expose = expose;
    }

    @Override
    public List<String> getPorts() {
        if (ports == null) {
            ports = new ArrayList<>();
        }
        return ports;
    }

    public void setPorts(List<String> ports) {
        this.ports = ports;
    }

    @Override
    public List<String> getVolumes() {
        if (volumes == null) {
            volumes = new ArrayList<>();
        }
        return volumes;
    }

    public void setVolumes(List<String> volumes) {
        this.volumes = volumes;
    }

    @Override
    public List<String> getVolumesFrom() {
        if (volumesFrom == null) {
            volumesFrom = new ArrayList<>();
        }
        return volumesFrom;
    }

    public void setVolumesFrom(List<String> volumesFrom) {
        this.volumesFrom = volumesFrom;
    }

    @Override
    public Integer getMemLimit() {
        return memLimit;
    }

    public void setMemLimit(Integer memLimit) {
        this.memLimit = memLimit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceImpl)) return false;
        ServiceImpl service = (ServiceImpl)o;
        return Objects.equals(containerName, service.containerName) &&
               Objects.equals(context, service.context) &&
               Objects.equals(dockerfile, service.dockerfile) &&
               Objects.equals(command, service.command) &&
               Objects.equals(entrypoint, service.entrypoint) &&
               Objects.equals(image, service.image) &&
               Objects.equals(dependsOn, service.dependsOn) &&
               Objects.equals(environment, service.environment) &&
               Objects.equals(expose, service.expose) &&
               Objects.equals(ports, service.ports) &&
               Objects.equals(labels, service.labels) &&
               Objects.equals(links, service.links) &&
               Objects.equals(volumes, service.volumes) &&
               Objects.equals(volumesFrom, service.volumesFrom) &&
               Objects.equals(memLimit, service.memLimit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(containerName,
                            context,
                            dockerfile,
                            command,
                            entrypoint,
                            image,
                            dependsOn,
                            environment,
                            expose,
                            ports,
                            labels,
                            links,
                            volumes,
                            volumesFrom,
                            memLimit);
    }

    @Override
    public String toString() {
        return "ServiceImpl{" +
               "containerName='" + containerName + '\'' +
               ", context='" + context + '\'' +
               ", dockerfile='" + dockerfile + '\'' +
               ", command=" + command +
               ", entrypoint=" + entrypoint +
               ", image='" + image + '\'' +
               ", dependsOn=" + dependsOn +
               ", environment=" + environment +
               ", expose=" + expose +
               ", ports=" + ports +
               ", labels=" + labels +
               ", links=" + links +
               ", volumes=" + volumes +
               ", volumesFrom=" + volumesFrom +
               ", memLimit=" + memLimit +
               '}';
    }
}
