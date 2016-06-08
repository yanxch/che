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
package org.eclipse.che.api.workspace.server.env.impl.che;

import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.machine.server.model.impl.MachineConfigImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineSourceImpl;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.testng.Assert.assertEquals;

/**
 * author Alexander Garagatyi
 */
public class DependenciesBasedCheEnvStartStrategyTest {
    private DependenciesBasedCheEnvStartStrategy strategy;

    @BeforeMethod
    public void setUp() throws Exception {
        strategy = new DependenciesBasedCheEnvStartStrategy();
    }

    @Test
    public void shouldNotChangeConfigsWithoutDependencies() throws Exception {
        List<String> expectedMachines = asList("m1", "m2", "devM", "m3");
        List<MachineConfig> machines = new ArrayList<>();
        for (String expectedMachineName : expectedMachines) {
            machines.add(createConfig(expectedMachineName));
        }
        ((MachineConfigImpl) machines.get(2)).setDev(true);

        List<MachineConfig> orderedMachnes = strategy.order(machines);

        assertEquals(orderedMachnes.stream().map(MachineConfig::getName).collect(Collectors.toList()), expectedMachines);
    }

    @Test(dataProvider = "validConfigsProvider")
    public void shouldBeAbleToOrderMachinesWithDependencies(Map<String, List<String>> machinesNamesDependencies,
                                                     int devPosition,
                                                     List<String> expectedOrder) throws Exception {
        List<MachineConfig> machines = new ArrayList<>();
        for (Map.Entry<String, List<String>> machineNameLink : machinesNamesDependencies.entrySet()) {
            machines.add(createConfig(machineNameLink.getKey(), machineNameLink.getValue()));
        }
        ((MachineConfigImpl) machines.get(devPosition)).setDev(true);

        List<MachineConfig> orderedMachnes = strategy.order(machines);

        assertEquals(orderedMachnes.stream().map(MachineConfig::getName).collect(Collectors.toList()), expectedOrder);
    }

    @DataProvider(name = "validConfigsProvider")
    public static Object[][] validConfigsProvider() {
        return new Object[][] {
                {new HashMap<String, List<String>>() {{
                    put("m1", emptyList());
                }}, 0, singletonList("m1")},
                {new HashMap<String, List<String>>() {{
                    put("m1", emptyList());
                    put("m4", singletonList("m1"));
                    put("m2", singletonList("m4"));
                    put("m3", singletonList("m2"));
                }}, 3, asList("m1", "m4", "m2", "m3")},
                {new HashMap<String, List<String>>() {{
                    put("m1", emptyList());
                    put("m2", singletonList("m1"));
                    put("m3", singletonList("m4"));
                    put("m4", singletonList("m2"));
                }}, 3, asList("m1", "m2", "m4", "m3")},
                {new HashMap<String, List<String>>() {{
                    put("m1", emptyList());
                    put("m2", asList("m1", "m3"));
                    put("m3", singletonList("m4"));
                    put("m4", emptyList());
                }}, 3, asList("m1", "m4", "m3", "m2")},
                {new HashMap<String, List<String>>() {{
                    put("m1", singletonList("m2"));
                    put("m2", singletonList("m3"));
                    put("m3", singletonList("m4"));
                    put("m4", emptyList());
                }}, 3, asList("m4", "m3", "m2", "m1")},
                {new HashMap<String, List<String>>() {{
                    put("m1", emptyList());
                    put("m2", asList("m1", "m3"));
                    put("m3", singletonList("m1"));
                    put("m4", singletonList("m3"));
                }}, 3, asList("m1", "m3", "m2", "m4")},
        };
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowExceptionIfMachineDependsOnItsOwn() throws Exception {
        List<String> expectedMachines = asList("m1", "m2");
        List<MachineConfig> machines = new ArrayList<>();
        machines.add(createConfig("m1"));
        machines.add(createConfig("m2", singletonList("m2")));
        ((MachineConfigImpl) machines.get(1)).setDev(true);

        List<MachineConfig> orderedMachnes = strategy.order(machines);

        assertEquals(orderedMachnes.stream().map(MachineConfig::getName).collect(Collectors.toList()), expectedMachines);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowExceptionInCaseOfCircularDependency() throws Exception {
        List<String> expectedMachines = asList("m1", "m2");
        List<MachineConfig> machines = new ArrayList<>();
        machines.add(createConfig("m1", singletonList("m2")));
        machines.add(createConfig("m2", singletonList("m1")));
        ((MachineConfigImpl) machines.get(1)).setDev(true);

        List<MachineConfig> orderedMachnes = strategy.order(machines);

        assertEquals(orderedMachnes.stream().map(MachineConfig::getName).collect(Collectors.toList()), expectedMachines);
    }

    private MachineConfigImpl createConfig(String machineName) {
        return MachineConfigImpl.builder()
                                .setDev(false)
                                .setName(machineName)
                                .setType("docker")
                                .setSource(new MachineSourceImpl("image").setLocation("codenvy/ubuntu_jdk8"))
                                .build();
    }

    private MachineConfigImpl createConfig(String machineName, List<String> dependencies) {
        return MachineConfigImpl.builder()
                                .setDev(false)
                                .setName(machineName)
                                .setType("docker")
                                .setSource(new MachineSourceImpl("image").setLocation("codenvy/ubuntu_jdk8"))
                                .setDependsOn(dependencies)
                                .build();
    }
}
