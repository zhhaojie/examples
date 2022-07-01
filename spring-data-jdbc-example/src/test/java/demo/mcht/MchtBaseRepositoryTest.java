package demo.mcht;


import demo.JdbcApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest(classes = JdbcApplication.class)
public class MchtBaseRepositoryTest {

    @Resource
    private MchtBaseRepository mchtBaseRepository;

    @Test
    void findByMchtIdEquals() {
        Long mchtId = 1000323L;
        final MchtBase byMchtIdEquals = mchtBaseRepository.findByMchtIdEquals(mchtId);
        System.out.println(byMchtIdEquals);

       // mchtBaseRepository.findAll(PageRequest.of(0, 10));
    }
}
