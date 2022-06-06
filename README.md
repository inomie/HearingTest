# HearingTest
## Description
This is part of my exam work for university. The purpose of this project is to create an Android mobile application to perform a standard audiometry and a specific psychoacoustic (temporal masking) to measure the user’s hearing profile and the hidden hearing loss. The application performs these tests over Bluetooth communication with a specific headphone (Sony WH-1000X M3). The user is responding to the sound by pressing or releasing a touch button on the phone screen. The application can send the results over Bluetooth to headphones over serial communication (SPP profile). The test result will then be used for tuning the sound coding parameters in the connected headphones according to the user’s individual hearing profile. 

## Implementation
### Pure-tone Audiometry
The algorithm needs to go through all the frequencies that we are using in this test (250, 500, 1000, 2000, 3000, 4000, 6000, 8000).

First figure shows the audiometry test of the right ear. It’s also showing that we are starting on 1000Hz and not 250Hz, this is because of the standards of the audiometry test that we need to follow. It also showing that the first frequency is starting on a higher decibel intensity than the other frequencies are. This is also one of the standards we need to follow in this procedure. 

The test starts on the first frequency that shows in figure 6. If the person who is taking the test is not pressing the button on the screen the decibel intensity will increase with 5dB every time it plays the tone.  If the user is pressing the button, the first value will not be stored just to get a better calculation of the threshold later. When the user is holding the button down the decibel intensity will decrease with 5dB every time it’s being played. When the user can’t hear the tone any more the user then releases the button, and the value of the decibel intensity will be stored. After that the user releases the button the decibel intensity will start to increase again but this time with 2dB instead of 5dB. The same thing is happening when the user is pressing the button down again but decreasing with 2dB. This is just to get a better calculation of the threshold.

First figure shows that five values of the decibel intensity are being stored. The five values are being used to calculate the threshold of that frequency. This will be repeated through all the frequencies. The only difference between them is that only the first frequency is starting on a decibel intensity of 30dB, the other frequencies start on a decibel intensity of 0dB.

Second figure shows the same test but on the left ear. The test works in the same way as the right ear does. The only difference is that the first frequency is not starting on a higher decibel intensity than the other frequencies.

![image](https://user-images.githubusercontent.com/78423305/172205939-70029608-07ce-47fc-afef-26741173076c.png)
![image](https://user-images.githubusercontent.com/78423305/172206000-b5665f54-29ee-4041-8451-ca95c8523bb0.png)

### Temporal masking
The implementation of the temporal masking test is a bit more complicated than the audiometry test. This is because the gap between the noise and the tone needs to be exactly in the millisecond to be correct, a system sleep or a thread sleep is not working to give an exact pause in milliseconds. To be able to fix this problem a function needs to be created to take a noise and a tone to combine them with the gap it needs to have in between. To be able to combine them a new header needs to be made. 

The test starts on the first frequency that shows in the picture bellow. If the person who is taking the test is not pressing the button on the screen the decibel intensity will be increasing with 5dB every time it plays the tone.  If the user is pressing the button, the first value will not be stored just to get a better calculation of the threshold later. When the user is holding the button down the decibel intensity of the tone will then decrease with 5dB every time it’s being played. When the user can’t hear the tone any more the user then releases the button, and the value of the decibel intensity will be stored. After that the user releases the button the decibel intensity will start to increase again but this time with 2dB instead of 5dB. The same thing is happening when the user is pressing the button down again but decreasing with 2dB. This is just to get a better calculation of the threshold.

The temporal masking that’s being used in this application can be seen in figure 3. It contains a masker (noise) followed by a tone with a specific gap between them. This temporal masking (noise, gap, tone) will be repeated until the application has stored five inputs from the user.

![image](https://user-images.githubusercontent.com/78423305/172206358-45c9acc4-e0b4-43fa-b8e2-8c5767c65681.png)

## Usage
Before taking the test the user needs to connect to the device sp the device is stored in paired devices. When the application is started a first page will pop up with some information about the tests. On the first page four buttons will be showned. Three of the button is for the sure to get used to the sounds. The fourth button is to go to next page. The user can't go to next page if the phone don't have Bluetooth or if it's not enable.

![image](https://user-images.githubusercontent.com/78423305/172207324-61939328-00e9-49c0-99d9-38772619f6f1.png)

The next page will show what test it will start on and on what ear it will be testing. The user press start when they ready.

![image](https://user-images.githubusercontent.com/78423305/172207604-1f52bc1b-3f9e-4006-beec-96785431f8ed.png)

When the user have pressed start the button will change to a button called press. This is the button the user will hold down when they can hear the tone and release whe they dont hear the tone any more.

![image](https://user-images.githubusercontent.com/78423305/172207866-791a5070-5da2-41f2-9d51-91605cc02f11.png)

When the user has complited the tests the button will disappear and a dropdown list will be showing all the paired devices. The user is then selecting the device (hadphones) and then pressing connect button. When the devices is connected a button will appear on the screen saying send. This button will send the test results over to the device. 


