package demo.mcht;

import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@ToString
@Table("TBL_MCHT_BASE")
public class MchtBase {

    @Id
    @Column("ID")
    private Long mchtId;

    /**
     * 商户Name
     */
    @Column("MCHT_NM")
    private String mchtName;

    /**
     * 商户No
     */
    @Column("MCHT_NO")
    private String mchtNo;

    /**
     * 商户机构号
     */
    @Column("MCHT_BRH")
    private String mchtBrh;
}
