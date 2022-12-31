#  [BETA] SiHAS (Korea) Zigbee Device drivers for Hubitat

This is a dedicated thread on the [SiHAS](https://sihas.co.kr/mprd) (Korea) Zigvee device drivers that are available in Hubitat.

Definitely, the most interesting and innovative product is the CSM-300-ZB (V2) "People Counter". It uses a [Time-of-Flight](https://en.wikipedia.org/wiki/Time_of_flight) principle sensor combined with an ordinary PIR sensor to count the number of the persons passing through a door. Controlling room lights on/off depending on the number of the people is a totally different approach compared to the traditional PIR and mmWave sensors methods.

All SiHAS drivers are available for installation and update from HPM (Hubitat Package Manager)


|  Device |  Links |
|---|---|
| CSM-300-ZB Wireless occupancy / people counter <b>ToF</b> sensor (Version 2)<br>![image](https://user-images.githubusercontent.com/6189950/201586405-3b2edde9-6783-4d88-92ad-722b754236f9.png) | Product [link1](https://smartstore.naver.com/sihas/products/5923346431) <br>Product [link2](https://sihas.co.kr/untitled-48) <br><br> HE driver: [link](https://raw.githubusercontent.com/kkossev/Hubitat-SiHAS/main/sihas_peoplecounter_csm300zb.groovy) |
| DMS-300-ZB Dual Motion Sensor <br> ![image](https://user-images.githubusercontent.com/6189950/201587147-a23647d4-97fc-47a8-a81f-b8763914a379.png) | Product [link1](https://sihas.co.kr/product/511ef84a-fc7a-4333-bf29-1a3ee8b39810) <br>Product [link2](https://sihas.co.kr/untitled-40) <br><br> HE driver: [link](https://raw.githubusercontent.com/kkossev/Hubitat-SiHAS/main/sihas_dualmotionsensor_dms300zb.groovy) |
| USM-300-ZB Multifunction sensor <br> ![image](https://user-images.githubusercontent.com/6189950/201587714-645d3321-2c36-4c65-abdf-2ec3ab57a083.png) | Product [link1](https://smartstore.naver.com/sihas/products/5928899987) <br>Product [link2](https://sihas.co.kr/untitled-41) <br><br> HE driver: [link](https://raw.githubusercontent.com/kkossev/Hubitat-SiHAS/main/sihas_multipurpose_usm300zb.groovy) |
| PMM-300-ZB Smart Power Meter Single Phase <br> ![image](https://user-images.githubusercontent.com/6189950/201587994-5ac64c25-b97d-44d1-9ced-c1ddf8f42048.png) | Product [link1](https://smartstore.naver.com/sihas/products/5090577864) <br>Product [link2](https://sihas.co.kr/untitled-31) <br><br> HE driver: [link](https://raw.githubusercontent.com/kkossev/Hubitat-SiHAS/main/sihas_powermeter_pmm300z.groovy) |
----------------------------
Many thanks to @jw970065 for providing the test sensors and the beta testing!




This is an automatic Google translation of part of the <b>Counter Sensor Season 2 release and information</b> published in Korean language [here](https://sihas.co.kr/boardPost/106349/37) 

Season 1, the first generation of counter sensors, used two PIR sensors to process counting, but the detection range was wide, causing many errors in the beginning. However, there were various errors because the PIR signal itself is based on temperature change, the detection signal is long, and there is also a sensitivity difference between the two sensors.

Unlike the 1st generation, which is PIR+PIR, Season 2, the 2nd generation counter sensor, is configured to process counting with PIR+ToF. In Season 2, PIR is used only as a pre-detection for battery saving, and the actual counting is processed only by the ToF sensor. Therefore, there is no error counting when there is no person like season 1 and error due to change in ambient temperature. In addition, using a high-speed laser-based ToF sensor, fast counting, counting by slow hesitation, and entering and exiting Counting errors due to coming out immediately have also been greatly improved.

And just like in season 1, the first generation, we conducted a preliminary test for about a month by calling a cafe test group (13 people). Through the test, not only the ST environment but also the HA environment have been further improved on various issues. (Season 2 tester Refer to the group survey) Nevertheless, Season 2 is also a sensor, so it is not perfect and there may be some errors depending on the usage environment. In case of counting errors, manual correction of counting or error correction automation in conjunction with other sensors may be required. Depending on the usage environment, the installation height and installation direction according to the user's movement line and the optimization setting of the counting transaction time can be set. Errors can be minimized.

** Season 2 counter sensor main features are as follows.
1. Function: Improved counting function by PIR+ToF sensor
2. Supported platforms: ST Edge driver, HA z2m (OTA support) (<b>EDIT</b>: now also Hubitat Elevation)
3. Power: USB-C type rechargeable lithium-ion battery (2000mA), use for about 1 year when detected every 10 minutes
4. Size: Smaller and slimmer than Season 1, 55Wx74Hx16D (including bracket) Unit: mm
5. Automatic environment setting according to the installation location with the distance measurement adjustment function according to the use environment (maximum use distance 2.4m)
6. Provides quick RF interlocking function with Sihas button type switch (SBM-300 Series)
7. Fast counting function provided for initial entry (0->1) and last exit (1->0)
8. Transaction interval setting function for optimization of continuous access (0 to 1 second, 6 steps)
9. Provides counting stop (freeze) function for selection of use condition in case of ST automation by counter value
10. Provides various setting functions (setting by button, setting by counter value, setting by ST app)
11. User manual and ST Edge driver (V2) installation support via QR code

Purchase page [link](https://sihas.co.kr/product/511ef84a-fc7a-4333-bf29-1a3ee8b39810-4) (You cannot purchase through Naver Pay)

