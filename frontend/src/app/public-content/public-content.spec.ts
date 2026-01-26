import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PublicContent } from './public-content';

describe('PublicContent', () => {
  let component: PublicContent;
  let fixture: ComponentFixture<PublicContent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PublicContent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PublicContent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
