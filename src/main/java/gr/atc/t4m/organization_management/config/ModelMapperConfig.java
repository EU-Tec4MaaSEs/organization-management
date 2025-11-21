package gr.atc.t4m.organization_management.config;


import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig{

  @Bean
  public ModelMapper modelMapper() {
    ModelMapper mapper = new ModelMapper();

    mapper.getConfiguration()
            .setSkipNullEnabled(true)
            .setPropertyCondition(Conditions.isNotNull())
            .setCollectionsMergeEnabled(false);

    return mapper;
}
}