package com.shipnolja.reservation.ship.service.Impl;

import com.shipnolja.reservation.fishinginfo.dto.response.ResFishingInfoListDto;
import com.shipnolja.reservation.fishinginfo.model.FishingInfo;
import com.shipnolja.reservation.fishinginfo.repository.FishingInfoRepository;
import com.shipnolja.reservation.reservation.dto.response.ResReservationListDto;
import com.shipnolja.reservation.reservation.model.Reservation;
import com.shipnolja.reservation.reservation.repository.ReservationRepository;
import com.shipnolja.reservation.ship.dto.response.ResManagerShipInfo;
import com.shipnolja.reservation.ship.dto.response.ResShipInfo;
import com.shipnolja.reservation.ship.dto.response.ResShipInfoList;
import com.shipnolja.reservation.ship.model.ShipInfo;
import com.shipnolja.reservation.ship.repository.ShipRepository;
import com.shipnolja.reservation.ship.service.ShipService;
import com.shipnolja.reservation.user.model.UserInfo;
import com.shipnolja.reservation.user.model.UserRole;
import com.shipnolja.reservation.user.repository.UserRepository;
import com.shipnolja.reservation.util.exception.CustomException;
import com.shipnolja.reservation.wish.repository.WishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShipServiceImpl implements ShipService {

    private final UserRepository userRepository;
    private final ShipRepository shipRepository;
    private final WishRepository wishRepository;
    private final FishingInfoRepository fishingInfoRepository;
    private final ReservationRepository reservationRepository;

    @Override
    public List<ResShipInfoList> shipList(String area, String detailArea, String port, String shipName, String sortBy, String sortMethod, int page) {
        
        Pageable pageable = null;

        if(sortMethod.equals("asc"))
            pageable = PageRequest.of(page, 10, Sort.by(sortBy).ascending());
        else if(sortMethod.equals("desc"))
            pageable = PageRequest.of(page, 10, Sort.by(sortBy).descending());

        Page<ShipInfo> shipInfoPage = shipRepository.findShipInfoList(
                shipName,port,area,detailArea,pageable
        );

        List<ShipInfo> shipInfoList = shipInfoPage.getContent();

        List<ResShipInfoList> shipInfoListDto = new ArrayList<>();

        shipInfoList.forEach(entity->{
            ResShipInfoList listDto = new ResShipInfoList();
            listDto.setId(entity.getId());
            listDto.setImage(entity.getImage());
            listDto.setName(entity.getName());
            listDto.setArea(entity.getArea());
            listDto.setDetailArea(entity.getDetailArea());
            listDto.setPort(entity.getPort());
            listDto.setStreetAddress(entity.getStreetAddress());
            listDto.setWishCount(wishRepository.countByShipInfo(entity));
            listDto.setTotalPage(shipInfoPage.getTotalPages());
            listDto.setTotalElement(shipInfoPage.getTotalElements());
            shipInfoListDto.add(listDto);
            }
        );

        return shipInfoListDto;
    }

    @Override
    public ResShipInfo shipInfo(Long id) {
        ShipInfo shipInfo = shipRepository.findById(id).orElseThrow(
                () -> new CustomException.ResourceNotFoundException("선상 정보를 찾을 수 없습니다")
        );


        return new ResShipInfo(shipInfo,wishRepository.countByShipInfo(shipInfo));
    }

    @Override
    public ResManagerShipInfo shipMangerInfo(Long id) {
        ShipInfo shipManagerInfo = shipRepository.findById(id).orElseThrow(
                () -> new CustomException.ResourceNotFoundException("선상 정보를 찾을 수 없습니다")
        );

        return new ResManagerShipInfo(shipManagerInfo);
    }

    /* 내 출조 정보 목록 조회 */
    @Override
    public List<ResFishingInfoListDto> managerFishingInfoList(UserInfo userInfo, Long ship_id, String sortMethod, String searchBy, String content, int page) {

        UserInfo checkUserInfo = userRepository.findByIdAndRole(userInfo.getId(), UserRole.ROLE_MANAGER)
                .orElseThrow(() -> new CustomException.ResourceNotFoundException("매니저 이용자만 사용할 수 있습니다."));

        ShipInfo checkShipInfo = shipRepository.findById(ship_id)
                .orElseThrow(() -> new CustomException.ResourceNotFoundException("선박 정보를 찾을 수 없습니다."));

        Pageable pageable = null;
        Page<FishingInfo> fishingInfoPage = null;

        List<ResFishingInfoListDto> fishingInfoList = new ArrayList<>();

        if(sortMethod.equals("asc")) {
            pageable = PageRequest.of(page,10, Sort.by("infoStartDate").ascending());
        } else if(sortMethod.equals("desc")) {
            pageable = PageRequest.of(page, 10, Sort.by("infoStartDate").descending());
        }
        
        switch (searchBy) {
            case "출조날짜" :
                LocalDate fishingInfoDate = LocalDate.parse(content, DateTimeFormatter.ISO_DATE);
                fishingInfoPage = fishingInfoRepository.findByShipInfoAndInfoStartDate(checkShipInfo, fishingInfoDate, pageable);
                break;
            case "예약상태" :
                fishingInfoPage = fishingInfoRepository.findByShipInfoAndInfoReservationStatusContaining(checkShipInfo, content, pageable);
                break;
        }

        if(fishingInfoPage != null) {

            int totalPages = fishingInfoPage.getTotalPages();
            long totalElements = fishingInfoPage.getTotalElements();

            fishingInfoPage.forEach(fishingInfo -> {

                ResFishingInfoListDto resFishingInfoListDto = new ResFishingInfoListDto();

                resFishingInfoListDto.setId(fishingInfo.getInfoId());
                resFishingInfoListDto.setArea(fishingInfo.getShipInfo().getArea());
                resFishingInfoListDto.setDetailArea(fishingInfo.getShipInfo().getDetailArea());
                resFishingInfoListDto.setPort(fishingInfo.getShipInfo().getPort());
                resFishingInfoListDto.setShipName(fishingInfo.getShipInfo().getName());
                resFishingInfoListDto.setTarget(fishingInfo.getInfoTarget());
                resFishingInfoListDto.setInfoStartDate(fishingInfo.getInfoStartDate());
                resFishingInfoListDto.setStartTime(fishingInfo.getInfoStartTime());
                resFishingInfoListDto.setEndTime(fishingInfo.getInfoEndTime());
                resFishingInfoListDto.setInfoReservationStatus(fishingInfo.getInfoReservationStatus());
                resFishingInfoListDto.setInfoCapacity(fishingInfo.getInfoCapacity());
                resFishingInfoListDto.setImage(fishingInfo.getShipInfo().getImage());
                resFishingInfoListDto.setTotalPage(totalPages);
                resFishingInfoListDto.setTotalElement(totalElements);

                fishingInfoList.add(resFishingInfoListDto);
            });
        }


        return fishingInfoList;
    }

    /* 매니저 출조 예약자 목록 */
    @Override
    public ResReservationListDto managerReservationList(UserInfo userInfo, Long ship_id, Long info_id, String sortMethod, String searchBy, String content, int page) {

        UserInfo checkUserInfo = userRepository.findByIdAndRole(userInfo.getId(), UserRole.ROLE_MANAGER)
                .orElseThrow(() -> new CustomException.ResourceNotFoundException("매니저 이용자만 사용할 수 있습니다."));

        ShipInfo checkShipInfo = shipRepository.findById(ship_id)
                .orElseThrow(() -> new CustomException.ResourceNotFoundException("선박 정보를 찾을 수 없습니다."));

        FishingInfo checkFishingInfo = fishingInfoRepository.findById(info_id)
                .orElseThrow(() -> new CustomException.ResourceNotFoundException("출조 정보를 찾을 수 없습니다."));

        Pageable pageable = null;
        Page<Reservation> reservationPage = null;

        List<ResReservationListDto> reservationList = new ArrayList<>();

        if(sortMethod.equals("asc")) {
            pageable = PageRequest.of(page,10, Sort.by("reservationDate").ascending());
        } else if(sortMethod.equals("desc")) {
            pageable = PageRequest.of(page, 10, Sort.by("reservationDate").descending());
        }

        return null;
    }
}
