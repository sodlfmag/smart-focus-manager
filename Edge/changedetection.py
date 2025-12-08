import os
import cv2
import pathlib
from pathlib import Path
import requests
from datetime import datetime

class ChangeDetection:
    HOST = 'https://sodlfmag.pythonanywhere.com'
    username = 'admin'
    password = 'admin'
    token = ''
    previous_state = None  # 이전 상태 저장 ('Focus', 'Distracted', 'Away')
    
    def __init__(self, names):
        res = requests.post(self.HOST + '/api-token-auth/', {
            'username': self.username,
            'password': self.password,
        })
        res.raise_for_status()
        self.token = res.json()['token']
        print(f"Token obtained: {self.token[:20]}...")
    
    def determine_state(self, detected_current, names):
        """
        Person(0)과 Cell Phone(67) 감지에 따른 상태 판단
        - Focus: person 감지 AND cell phone 없음
        - Distracted: person 감지 AND cell phone 감지
        - Away: person 없음
        """
        # COCO 데이터셋: Person 클래스 ID = 0, Cell Phone 클래스 ID = 67
        PERSON_CLASS_ID = 0
        PHONE_CLASS_ID = 67
        
        person_detected = False
        phone_detected = False
        
        # names 딕셔너리에서 클래스 ID 찾기 (다른 데이터셋 대비)
        person_id = None
        phone_id = None
        
        # names가 딕셔너리인 경우 (YOLOv5 기본)
        if isinstance(names, dict):
            for class_id, name in names.items():
                name_lower = str(name).lower()
                if 'person' in name_lower:
                    person_id = class_id
                elif 'cell phone' in name_lower or 'phone' in name_lower:
                    phone_id = class_id
        # names가 리스트인 경우 (다른 형태의 데이터셋)
        else:
            for idx, name in enumerate(names):
                name_lower = str(name).lower()
                if 'person' in name_lower:
                    person_id = idx
                elif 'cell phone' in name_lower or 'phone' in name_lower:
                    phone_id = idx
        
        # COCO 데이터셋의 경우 인덱스가 클래스 ID와 일치하므로 직접 사용
        # 하지만 다른 데이터셋을 위해 names에서 찾은 인덱스도 사용
        if person_id is None:
            person_id = PERSON_CLASS_ID if PERSON_CLASS_ID < len(detected_current) else None
        if phone_id is None:
            phone_id = PHONE_CLASS_ID if PHONE_CLASS_ID < len(detected_current) else None
        
        # 감지 여부 확인 (detected_current는 클래스 ID를 인덱스로 하는 리스트)
        if isinstance(detected_current, list):
            if person_id is not None and person_id < len(detected_current):
                person_detected = (detected_current[person_id] == 1)
            
            if phone_id is not None and phone_id < len(detected_current):
                phone_detected = (detected_current[phone_id] == 1)
        else:
            # detected_current가 딕셔너리인 경우 (다른 형태의 데이터셋)
            if person_id is not None:
                person_detected = (detected_current.get(person_id, 0) == 1)
            if phone_id is not None:
                phone_detected = (detected_current.get(phone_id, 0) == 1)
        
        # 상태 판단
        if person_detected and phone_detected:
            return 'Distracted'
        elif person_detected and not phone_detected:
            return 'Focus'
        else:
            return 'Away'
    
    def add(self, names, detected_current, save_dir, image):
        """
        현재 상태를 판단하고, 이전 상태와 다르면 서버로 전송
        """
        current_state = self.determine_state(detected_current, names)
        
        # 상태가 변경되었을 때만 전송
        if current_state != self.previous_state:
            print(f"State changed: {self.previous_state} -> {current_state}")
            self.send(current_state, save_dir, image)
            self.previous_state = current_state
    
    def send(self, state, save_dir, image):
        """
        상태 변경 시 서버로 이미지와 상태 정보 전송
        """
        now = datetime.now()
        today = datetime.now()
        
        # save_dir이 Path 객체일 수 있으므로 str로 변환
        if isinstance(save_dir, (pathlib.Path, Path)):
            save_dir_str = str(save_dir)
        else:
            save_dir_str = save_dir
        
        # 이미지 저장 경로 (blog_image로 변경)
        save_path = Path(save_dir_str) / 'blog_image' / str(today.year) / str(today.month) / str(today.day)
        pathlib.Path(save_path).mkdir(parents=True, exist_ok=True)
        full_path = save_path / '{0}-{1}-{2}-{3}.jpg'.format(today.hour, today.minute, today.second, today.microsecond)
        
        # 이미지 리사이즈 및 저장
        dst = cv2.resize(image, dsize=(320, 240), interpolation=cv2.INTER_AREA)
        cv2.imwrite(str(full_path), dst)
        
        # 인증이 필요한 요청에 아래의 headers를 붙임
        headers = {'Authorization': 'Token ' + self.token, 'Accept': 'application/json'}
        
        # Post Create - 상태를 title에 저장
        data = {
            'title': state,  # 'Focus', 'Distracted', 'Away'
            'text': f'State: {state}',
            'created_date': now.isoformat(),
            'published_date': now.isoformat(),
            'author': 1
        }
        
        file = {'image': open(str(full_path), 'rb')}
        try:
            res = requests.post(self.HOST + '/api_root/Post/', data=data, files=file, headers=headers)
            print(f"Status sent: {state}, Response: {res.status_code}")
        except Exception as e:
            print(f"Error sending status: {e}")
        finally:
            file['image'].close()

