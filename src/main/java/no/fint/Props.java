package no.fint;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class Props {

    @Value("${fint.azure.aks.auth.clientId}")
    private String clientId;

    @Value("${fint.azure.aks.auth.clientSecret}")
    private String clientSecret;

    @Value("${fint.azure.aks.auth.credential-file}")
    private String credentialFile;

}
