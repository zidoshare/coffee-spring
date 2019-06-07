package site.zido.coffee.entity;

import java.io.Serializable;
import java.util.Collection;

public interface IUser extends Serializable {
    /**
     * 获取用户角色 (不使用get,不参与序列化)
     * @return 用户角色集合
     */
    default Collection<String> roles(){
        return null;
    }

    /**
     * 获取用户权限 (不使用get，不参与序列化)
     * @return 用户权限集合
     */
    default Collection<String> permissions(){
        return null;
    }
}
