import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PrivateContent } from './private-content';

describe('PrivateContent', () => {
  let component: PrivateContent;
  let fixture: ComponentFixture<PrivateContent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PrivateContent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PrivateContent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
