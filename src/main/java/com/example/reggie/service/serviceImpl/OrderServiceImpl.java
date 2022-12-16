package com.example.reggie.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.reggie.common.BaseContext;
import com.example.reggie.common.CustomException;
import com.example.reggie.entity.*;
import com.example.reggie.mapper.OrderMapper;
import com.example.reggie.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderDetailService orderDetailService;


    /**
     * 用户下单
     * @param orders
     */
    @Override
    @Transactional
    public void submit(Orders orders) {
        // 工具类事先生成订单id
        long orderId = IdWorker.getId();

        // 获取当前用户id
        Long userId = BaseContext.getCurrentId();

        // 获取购物车信息
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCart::getUserId, userId);
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(wrapper);
        if (shoppingCarts == null || shoppingCarts.size() == 0){
            throw new CustomException("购物车为空，不能下单");
        }

        // 查询用户信息
        User user = userService.getById(userId);

        // 根据用户查询地址数据
        AddressBook addressBook = addressBookService.getById(orders.getAddressBookId());
        if (addressBook == null){
            throw new CustomException("地址为空，不能下单");
        }

        // 计算金额
        AtomicInteger amount = new AtomicInteger(0);

        // 处理订单细节
        List<OrderDetail> orderDetails = shoppingCarts.stream().map((item) -> {
            OrderDetail orderDetail = new OrderDetail();
            // 将购物车中的数据复制到订单细节中
            BeanUtils.copyProperties(item, orderDetail);
            // 设置id，与orderDetail里的orderId 和 order的主键Id保持一致
            orderDetail.setOrderId(orderId);
            //累计金额到原子整形amount上
            amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());

            return orderDetail;
        }).collect(Collectors.toList());

        // 向订单表插入一条数据
        orders.setId(orderId);
        orders.setNumber(String.valueOf(orders.getId())); //number是购物车id
        orders.setStatus(2);  //订单状态 1待付款，2待派送，3已派送，4已完成，5已取消
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setAmount(new BigDecimal(amount.get())); //设置总金额，在后端计算，防止篡改
        orders.setUserName(user.getName());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress((addressBook.getProvinceName()==null ? "":addressBook.getProvinceName())
                        + (addressBook.getCityName()==null ? "":addressBook.getCityName())
                        + (addressBook.getDistrictName()==null ? "":addressBook.getDistrictName())
                        + (addressBook.getDetail()==null ? "":addressBook.getDetail()));
        orders.setConsignee(addressBook.getConsignee());
        this.save(orders);

        // 向订单明细表插入多条数据
        orderDetailService.saveBatch(orderDetails);

        // 清空购物车
        shoppingCartService.remove(wrapper);
    }
}
