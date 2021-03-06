package org.arquillian.cube.docker.impl.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.CubeContainers;
import org.arquillian.cube.docker.impl.client.config.ExposedPort;
import org.arquillian.cube.docker.impl.client.config.Image;
import org.arquillian.cube.docker.impl.client.config.Link;
import org.arquillian.cube.docker.impl.client.config.PortBinding;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

public final class ConfigUtil {

    private ConfigUtil() {}

    public static String[] trim(String[] data) {
        List<String> result = new ArrayList<String>();
        for(String val : data) {
            String trimmed = val.trim();
            if(!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result.toArray(new String[]{});
    }

    public static String[] reverse(String[] cubeIds) {
        String[] result = new String[cubeIds.length];
        int n = cubeIds.length-1;
        for(int i = 0; i < cubeIds.length; i++) {
            result[n--] = cubeIds[i];
        }
        return result;
    }

    public static String dump(CubeContainers containers) {
        Yaml yaml = new Yaml(new CubeRepresenter());
        return yaml.dump(containers);
    }

    private static class CubeRepresenter extends Representer {
        public CubeRepresenter() {
            this.representers.put(PortBinding.class, new ToStringRepresent());
            this.representers.put(ExposedPort.class, new ToStringRepresent());
            this.representers.put(Image.class, new ToStringRepresent());
            this.representers.put(Link.class, new ToStringRepresent());
            addClassTag(CubeContainers.class, Tag.MAP);
        }

        public class ToStringRepresent implements Represent {
            @Override
            public Node representData(Object data) {
                return representScalar(Tag.STR, data.toString());
            }
        }

        @Override
        protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
            if(propertyValue == null) {
                return null;
            }
            return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
        }
    }

    public static CubeContainers load(String content) {
        return load(new ByteArrayInputStream(content.getBytes()));
    }

    @SuppressWarnings("unchecked")
    public static CubeContainers load(InputStream inputStream) {
        // TODO: Figure out how to map root Map<String, Type> objects. Workaround by mapping it to Map structure then dumping it into individual objects
        Yaml yaml = new Yaml(new CubeConstructor());
        Map<String, Object> rawContainers = (Map<String, Object>) yaml.load(inputStream);

        CubeContainers containers = new CubeContainers();

        for(Map.Entry<String, Object> rawContainerEntry : rawContainers.entrySet()) {
            CubeContainer container = yaml.loadAs(yaml.dump(rawContainerEntry.getValue()), CubeContainer.class);
            containers.add(rawContainerEntry.getKey(), container);
        }
        return applyExtendsRules(containers);
    }

    public static class CubeConstructor extends Constructor {
        public CubeConstructor() {
            this.yamlClassConstructors.put(NodeId.scalar, new CubeMapping());
        }

        private class CubeMapping extends Constructor.ConstructScalar {

            @Override
            public Object construct(Node node) {
                if(node.getType() == PortBinding.class) {
                    String value = constructScalar((ScalarNode)node).toString();
                    return PortBinding.valueOf(value);
                } else if(node.getType() == ExposedPort.class) {
                    String value = constructScalar((ScalarNode)node).toString();
                    return ExposedPort.valueOf(value);
                } else if(node.getType() == Image.class) {
                    String value = constructScalar((ScalarNode)node).toString();
                    return Image.valueOf(value);
                } else if(node.getType() == Link.class) {
                    String value = constructScalar((ScalarNode)node).toString();
                    return Link.valueOf(value);
                }
                return super.construct(node);
            }
        }
    }

    private static CubeContainers applyExtendsRules(CubeContainers cubeContainers) {

        for(Map.Entry<String, CubeContainer> containerEntry : cubeContainers.getContainers().entrySet()) {
            CubeContainer container = containerEntry.getValue();
            if(container.getExtends() != null) {
                String extendsContainer = container.getExtends();
                if(cubeContainers.get(extendsContainer) == null) {
                    throw new IllegalArgumentException(containerEntry.getKey() + " extends a non existing container definition " + extendsContainer);
                }
                CubeContainer extendedContainer = cubeContainers.get(extendsContainer);
                container.merge(extendedContainer);
            }
        }
        return cubeContainers;
    }
}
