package example.mcht;


import org.springframework.data.repository.CrudRepository;

public interface MchtBaseRepository extends CrudRepository<MchtBase, Long> {
    MchtBase findByMchtIdEquals(Long mchtId);
}
