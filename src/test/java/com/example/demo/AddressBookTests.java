package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dao.UserAddressDAO;
import com.example.demo.entity.UserAddress;
import com.example.demo.form.UserAddressForm;

@SpringBootTest
@Transactional
public class AddressBookTests {

    @Autowired
    private UserAddressDAO userAddressDAO;

    @Test
    public void testFirstAddressIsAutoDefault() {
        String testUser = "unit_user_1";

        UserAddressForm form = new UserAddressForm();
        form.setReceiverName("Nguyễn Văn A");
        form.setPhone("0988111222");
        form.setProvince("Hà Nội");
        form.setDistrict("Quận Ba Đình");
        form.setWard("Phường Kim Mã");
        form.setStreetAddress("100 Kim Mã");
        form.setDefault(false); // Even if false, first address becomes default

        UserAddress saved = userAddressDAO.saveAddress(testUser, form);
        assertNotNull(saved);
        assertTrue(saved.isDefault());
    }

    @Test
    public void testUnsetPreviousDefaultAddress() {
        String testUser = "unit_user_2";

        // Address 1
        UserAddressForm form1 = new UserAddressForm();
        form1.setReceiverName("Địa chỉ 1");
        form1.setPhone("0900000001");
        form1.setProvince("Hồ Chí Minh");
        form1.setDistrict("Quận 1");
        form1.setWard("Phường Bến Nghé");
        form1.setStreetAddress("1 Lê Lợi");
        form1.setDefault(true);
        UserAddress addr1 = userAddressDAO.saveAddress(testUser, form1);

        // Address 2 (set default = true)
        UserAddressForm form2 = new UserAddressForm();
        form2.setReceiverName("Địa chỉ 2");
        form2.setPhone("0900000002");
        form2.setProvince("Hồ Chí Minh");
        form2.setDistrict("Quận 3");
        form2.setWard("Phường 6");
        form2.setStreetAddress("200 Võ Văn Tần");
        form2.setDefault(true);
        UserAddress addr2 = userAddressDAO.saveAddress(testUser, form2);

        // Verify addr1 is now NOT default, addr2 IS default
        UserAddress updatedAddr1 = userAddressDAO.getAddressById(addr1.getId());
        UserAddress updatedAddr2 = userAddressDAO.getAddressById(addr2.getId());

        assertFalse(updatedAddr1.isDefault());
        assertTrue(updatedAddr2.isDefault());
    }

    @Test
    public void testOwnershipEnforcementOnDelete() {
        String ownerUser = "owner_user";
        String intruderUser = "intruder_user";

        UserAddressForm form = new UserAddressForm();
        form.setReceiverName("Chính chủ");
        form.setPhone("0912345678");
        form.setProvince("Đà Nẵng");
        form.setDistrict("Quận Hải Châu");
        form.setWard("Phường Hòa Cường");
        form.setStreetAddress("50 Nguyễn Văn Linh");
        UserAddress addr = userAddressDAO.saveAddress(ownerUser, form);

        // Intruder tries to delete owner's address -> Should fail
        boolean deleteResult = userAddressDAO.deleteAddress(intruderUser, addr.getId());
        assertFalse(deleteResult);

        // Address should still exist in database
        assertNotNull(userAddressDAO.getAddressById(addr.getId()));

        // Owner deletes -> Should succeed
        boolean ownerDeleteResult = userAddressDAO.deleteAddress(ownerUser, addr.getId());
        assertTrue(ownerDeleteResult);
        assertNull(userAddressDAO.getAddressById(addr.getId()));
    }
}
