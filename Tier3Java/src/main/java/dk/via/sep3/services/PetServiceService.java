package dk.via.sep3.services;

import dk.via.sep3.DAOInterfaces.AnnouncementDAOInterface;
import dk.via.sep3.DAOInterfaces.PetServiceDAOInterface;
import dk.via.sep3.DAOInterfaces.PetServiceRequestDAOInterface;
import dk.via.sep3.DAOInterfaces.UserDAOInterface;
import dk.via.sep3.mappers.PetServiceMapper;
import dk.via.sep3.mappers.PetServiceRequestMapper;
import dk.via.sep3.shared.*;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import origin.protobuf.*;
import origin.protobuf.FindAnnouncementProto;
import origin.protobuf.FindRequestServiceProto;
import origin.protobuf.FindServiceProto;
import origin.protobuf.RequestServicesProto;
import origin.protobuf.SearchServiceProto;
import origin.protobuf.ServiceProto;
import origin.protobuf.ServiceRequestProto;
import origin.protobuf.ServicesProto;
import origin.protobuf.Void;

import javax.transaction.Transactional;
import java.util.Collection;

import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

@GRpcService
public class PetServiceService extends ServiceServiceGrpc.ServiceServiceImplBase
{
    private final UserDAOInterface userDao;
    private final AnnouncementDAOInterface announcementDAO;
    private final PetServiceRequestDAOInterface careServiceRequestDAO;
    private final PetServiceDAOInterface careServiceDAO;

    @Autowired
    public PetServiceService(UserDAOInterface userDao, AnnouncementDAOInterface announcementDAO, PetServiceRequestDAOInterface careServiceRequestDAO, PetServiceDAOInterface careServiceDAO)
    {
        this.userDao = userDao;
        this.announcementDAO = announcementDAO;
        this.careServiceRequestDAO = careServiceRequestDAO;
        this.careServiceDAO = careServiceDAO;
    }

    @Override
    @Transactional
    public void requestStartService(ServiceRequestProto request, StreamObserver<Void> responseObserver)
    {
        UserEntity initiator = userDao.findUser(request.getInitiatorEmail());
        UserEntity recipient = userDao.findUser(request.getRecipientEmail());
        AnnouncementEntity announcement = announcementDAO.getAnnouncement(request.getAnnouncementId());
        var serviceRequest = careServiceRequestDAO.createServiceRequest(new PetServiceRequestEntity(initiator,recipient,announcement));

        if(serviceRequest == null)
            responseObserver.onError(GrpcError.constructException("Can't offer care service"));
    }

    @Override
    @Transactional
    public void acceptStartService(FindRequestServiceProto request, StreamObserver<Void> responseObserver)
    {
        careServiceRequestDAO.confirmServiceRequest(request.getRequestId());
        PetServiceRequestEntity serviceRequest = careServiceRequestDAO.getServiceRequestById(request.getRequestId());
        UserEntity initiator = userDao.findUser(serviceRequest.getInitiator().getEmail());
        UserEntity recipient = userDao.findUser(serviceRequest.getRecipient().getEmail());

        CareTakerEntity careTaker;
        PetOwnerEntity petOwner;

        if (initiator instanceof CareTakerEntity) {
            careTaker = (CareTakerEntity) initiator;
            petOwner = (PetOwnerEntity) recipient;
        }
        else {
            petOwner = (PetOwnerEntity) initiator;
            careTaker = (CareTakerEntity) recipient;
        }

        careServiceDAO.createService(new PetServiceEntity(
                careTaker,
                petOwner,
                serviceRequest.getAnnouncement()
        ));
    }

    @Override
    @Transactional
    public void denyStartService(FindRequestServiceProto request, StreamObserver<Void> responseObserver)
    {
        careServiceRequestDAO.denyServiceRequest(request.getRequestId());
    }

    @Override
    @Transactional
    public void endService(FindServiceProto request, StreamObserver<Void> responseObserver)
    {
        careServiceDAO.endService(request.getServiceId());
    }

    @Override
    @Transactional
    public void searchRequestServices(FindAnnouncementProto request, StreamObserver<RequestServicesProto> responseObserver)
    {
        Collection<PetServiceRequestEntity> requests = careServiceRequestDAO.searchServiceRequests(request.getId());

        if (requests.isEmpty())
        {
            responseObserver.onError(GrpcError.constructException("No such requests"));
            return;
        }

        Collection<ServiceRequestProto> requestsCollection = requests
                .stream().map(PetServiceRequestMapper::mapToProto).toList();

        RequestServicesProto requestsProtoItems = RequestServicesProto.newBuilder().addAllRequestServices(requestsCollection).build();
        responseObserver.onNext(requestsProtoItems);
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void searchServices(SearchServiceProto request, StreamObserver<ServicesProto> responseObserver)
    {
        Collection<PetServiceEntity> services = careServiceDAO.searchServices(
                new CareTakerEntity(userDao.findUser(request.getCaretakerEmail())),
                new PetOwnerEntity(userDao.findUser(request.getPetOwnerEmail())),
                request.getStatus()
        );

        if (services.isEmpty())
        {
            responseObserver.onError(GrpcError.constructException("No such services"));
            return;
        }

        Collection<ServiceProto> servicessCollection = services
                .stream().map(PetServiceMapper::mapToProto).toList();

        ServicesProto servicesProtoItems = ServicesProto.newBuilder().addAllServices(servicessCollection).build();
        responseObserver.onNext(servicesProtoItems);
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void findService(FindServiceProto request, StreamObserver<ServiceProto> responseObserver)
    {
        responseObserver.onNext(PetServiceMapper.mapToProto(careServiceDAO.findServiceById(request.getServiceId())));
        responseObserver.onCompleted();
    }
}
