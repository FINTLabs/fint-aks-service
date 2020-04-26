package no.fint.aks.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.client.internal.serializationmixins.ObjectMetaMixIn;
import io.fabric8.kubernetes.client.internal.serializationmixins.ReplicationControllerMixIn;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SerializationUtils {

    private static ObjectMapper mapper;

    private static ObjectMapper statelessMapper;

    public static ObjectMapper getStatelessMapper() {
        if (statelessMapper == null) {
            statelessMapper = new ObjectMapper(new YAMLFactory());
            statelessMapper.addMixInAnnotations(ObjectMeta.class, ObjectMetaMixIn.class);
            statelessMapper.addMixInAnnotations(ReplicationController.class, ReplicationControllerMixIn.class);
        }
        return statelessMapper;
    }

    public static ObjectMapper getMapper() {
        if (mapper == null) {
            mapper = new ObjectMapper(new YAMLFactory());
        }
        return mapper;
    }

    public static String dumpAsYaml(HasMetadata obj)  {
        try {
            return getMapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public static String dumpWithoutRuntimeStateAsYaml(HasMetadata obj) throws JsonProcessingException {
        return getStatelessMapper().writeValueAsString(obj);
    }

}