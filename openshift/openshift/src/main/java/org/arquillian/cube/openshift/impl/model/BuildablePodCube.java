package org.arquillian.cube.openshift.impl.model;

import static org.arquillian.cube.openshift.impl.client.ResourceUtil.isRunning;
import static org.arquillian.cube.openshift.impl.client.ResourceUtil.toBinding;

import java.util.List;

import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;
import org.arquillian.cube.openshift.impl.client.OpenShiftClient;
import org.arquillian.cube.openshift.impl.client.OpenShiftClient.ResourceHolder;
import org.arquillian.cube.spi.BaseCube;
import org.arquillian.cube.spi.Binding;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeControlException;
import org.arquillian.cube.spi.metadata.IsBuildable;

import io.fabric8.kubernetes.api.model.Pod;

public class BuildablePodCube extends BaseCube<Void> {

    private String id;
    private Pod resource;
    private Template<Pod> template;
    private Cube.State state;
    private CubeOpenShiftConfiguration configuration;
    private OpenShiftClient client;

    private ResourceHolder holder;

    public BuildablePodCube(Pod resource, OpenShiftClient client, CubeOpenShiftConfiguration configuration) {
        this.id = resource.getMetadata().getName();
        this.resource = resource;
        this.template = new Template.PodTemplate(resource);
        this.client = client;
        this.configuration = configuration;
        addDefaultMetadata();
    }

    private void addDefaultMetadata() {
        if(template.getRefs() != null && template.getRefs().size() > 0) {
            addMetadata(IsBuildable.class, new IsBuildable(template.getRefs().get(0).getPath()));
        }
    }

    @Override
    public State state() {
        return state;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void create() throws CubeControlException {
        try {
            holder = client.build(template);
            this.state = State.CREATED;
        } catch (Exception e) {
            this.state = State.CREATE_FAILED;
            throw CubeControlException.failedCreate(getId(), e);
        }
    }

    @Override
    public void start() throws CubeControlException {
        try {
            holder.setPod(client.createAndWait(holder.getPod()));
            this.state = State.STARTED;
        } catch (Exception e) {
            this.state = State.START_FAILED;
            throw CubeControlException.failedStart(getId(), e);
        }
    }

    @Override
    public void stop() throws CubeControlException {
        try {
            client.destroy(holder.getPod());
            this.state = State.STOPPED;
        } catch (Exception e) {
            this.state = State.STOP_FAILED;
            throw CubeControlException.failedStop(getId(), e);
        }
    }

    @Override
    public void destroy() throws CubeControlException {
        try {
            List<Exception> exceptions = client.clean(holder);
            if (exceptions.size() > 0) {
                throw exceptions.get(0);
            }
            this.state = State.DESTROYED;
        } catch (Exception e) {
            this.state = State.DESTORY_FAILED;
            throw CubeControlException.failedDestroy(getId(), e);
        }
    }

    @Override
    public boolean isRunningOnRemote() {
        try {
            resource = client.update(resource);
            return isRunning(resource);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void changeToPreRunning() {
        // TODO Auto-generated method stub

    }

    @Override
    public Binding bindings() {
        if (holder != null) {
            return toBinding(holder.getPod());
        }
        return null;
    }

    @Override
    public Binding configuredBindings() {
        return toBinding(resource);
    }

    @Override
    public Void configuration() {
        return null;
    }
}
