/*
 *  Copyright 2016 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.idea;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerAdapter;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import org.jetbrains.annotations.NotNull;
import org.teavm.idea.jps.model.TeaVMJpsRemoteConfiguration;
import org.teavm.idea.jps.remote.TeaVMBuilderAssistant;
import org.teavm.idea.jps.remote.TeaVMElementLocation;

public class TeaVMJPSRemoteService implements ApplicationComponent, TeaVMBuilderAssistant {
    private ProjectManager projectManager = ProjectManager.getInstance();
    private int port;
    private Registry registry;

    @Override
    public void initComponent() {

        for (Project project : projectManager.getOpenProjects()) {
            configureProject(project);
        }
        projectManager.addProjectManagerListener(new ProjectManagerAdapter() {
            @Override
            public void projectOpened(Project project) {
                configureProject(project);
            }
        });
    }

    private void configureProject(Project project) {
        try {
            registry = LocateRegistry.createRegistry(0);
            registry.bind("TeaVM", this);
        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        }
        TeaVMRemoteConfigurationStorage storage = project.getComponent(TeaVMRemoteConfigurationStorage.class);
        TeaVMJpsRemoteConfiguration config = storage.getState();
        config.setPort(port);
        storage.loadState(config);
    }

    @Override
    public void disposeComponent() {
        try {
            registry.unbind("TeaVM");
            UnicastRemoteObject.unexportObject(registry, true);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "TeaVM JPS service";
    }

    @Override
    public TeaVMElementLocation getMethodLocation(String className, String methodName, String methodDesc)
            throws RemoteException {
        for (Project project : projectManager.getOpenProjects()) {
            JavaPsiFacade psi = JavaPsiFacade.getInstance(project);
            PsiClass cls = psi.findClass(className, GlobalSearchScope.allScope(project));
            if (cls == null) {
                continue;
            }

            for (PsiMethod method : cls.getAllMethods()) {
                if (!method.getName().equals(methodName)) {
                    continue;
                }
                // TODO: check method raw signature
                return getMethodLocation(method);
            }
        }
        return null;
    }

    private TeaVMElementLocation getMethodLocation(PsiMethod method) {
        return new TeaVMElementLocation(method.getTextOffset(), method.getTextOffset() + method.getTextLength(),
                -1, -1);
    }
}