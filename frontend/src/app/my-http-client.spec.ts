import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { MyHttpClient } from './my-http-client'; // Matches your filename

describe('MyHttpClient', () => {
  let service: MyHttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MyHttpClient,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(MyHttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  afterEach(() => {
    // Ensures that there are no outstanding requests between tests
    httpMock.verify();
  });
});
