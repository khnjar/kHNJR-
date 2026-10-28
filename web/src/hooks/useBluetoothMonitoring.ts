import { useState, useCallback } from 'react';

export const useBluetoothMonitoring = () => {
  const [data, setData] = useState<{ heartRate?: number; spo2?: number }>({});
  const [isConnected, setIsConnected] = useState(false);

  const connectBluetooth = useCallback(async () => {
    try {
      const device = await navigator.bluetooth.requestDevice({
        filters: [{ services: ['heart_rate'] }]
      });
      const server = await device.gatt?.connect();
      const service = await server?.getPrimaryService('heart_rate');
      const characteristic = await service?.getCharacteristic('heart_rate_measurement');
      
      await characteristic?.startNotifications();
      characteristic?.addEventListener('characteristicvaluechanged', (event: any) => {
        const value = event.target.value;
        // Simple parsing of standard Heart Rate characteristic
        const heartRate = value.getUint8(1);
        setData(prev => ({ ...prev, heartRate }));
      });
      
      setIsConnected(true);
    } catch (error) {
      console.error('Bluetooth connection failed', error);
    }
  }, []);

  return { isConnected, data, connectBluetooth };
};
