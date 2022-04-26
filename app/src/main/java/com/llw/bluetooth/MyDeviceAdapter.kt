package com.llw.bluetooth

import android.bluetooth.BluetoothDevice
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseDataBindingHolder
import com.llw.bluetooth.databinding.ItemDeviceBinding

/**
 * 设备适配器
 */
class MyDeviceAdapter(data: MutableList<MyDevice>) :
    BaseQuickAdapter<MyDevice, BaseDataBindingHolder<ItemDeviceBinding>>(R.layout.item_device, data) {
    override fun convert(holder: BaseDataBindingHolder<ItemDeviceBinding>, item: MyDevice) {
        holder.dataBinding?.apply {
            device = item
            executePendingBindings()
        }
    }
}